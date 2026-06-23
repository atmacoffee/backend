package com.atma.auth_service.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atma.auth_service.dto.ForgotPasswordResponse;
import com.atma.auth_service.dto.LoginRequest;
import com.atma.auth_service.dto.RegisterRequest;
import com.atma.auth_service.dto.UpdateProfileRequest;
import com.atma.auth_service.dto.VerifyResetCodeResponse;
import com.atma.auth_service.exception.TooManyRequestsException;
import com.atma.auth_service.model.PasswordResetRequest;
import com.atma.auth_service.model.Pengguna;
import com.atma.auth_service.model.Token;
import com.atma.auth_service.repository.PasswordResetRequestRepository;
import com.atma.auth_service.repository.PenggunaRepository;
import com.atma.auth_service.repository.TokenRepository;
import com.atma.auth_service.security.JwtUtil;

@Service
public class AuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int RESET_CODE_LENGTH = 6;
    private static final int RESET_SESSION_BYTES = 32;
    private static final String FORGOT_PASSWORD_GENERIC_MESSAGE =
            "Jika email terdaftar, kode reset password akan dikirim";

    private final PenggunaRepository penggunaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TokenRepository tokenRepository;
    private final PasswordResetRequestRepository passwordResetRequestRepository;
    private final PasswordResetMailService passwordResetMailService;
    private final NotificationService notificationService;
    private final int passwordResetCodeTtlMinutes;
    private final int passwordResetMaxAttempts;
    private final int passwordResetRequestCooldownSeconds;
    private final int passwordResetSessionTtlMinutes;

    public AuthService(
            PenggunaRepository penggunaRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TokenRepository tokenRepository,
            PasswordResetRequestRepository passwordResetRequestRepository,
            PasswordResetMailService passwordResetMailService,
            NotificationService notificationService,
            @Value("${app.password-reset.code-ttl-minutes:10}") int passwordResetCodeTtlMinutes,
            @Value("${app.password-reset.max-attempts:5}") int passwordResetMaxAttempts,
            @Value("${app.password-reset.request-cooldown-seconds:60}") int passwordResetRequestCooldownSeconds,
            @Value("${app.password-reset.session-ttl-minutes:10}") int passwordResetSessionTtlMinutes) {
        this.penggunaRepository = penggunaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.tokenRepository = tokenRepository;
        this.passwordResetRequestRepository = passwordResetRequestRepository;
        this.passwordResetMailService = passwordResetMailService;
        this.notificationService = notificationService;
        this.passwordResetCodeTtlMinutes = passwordResetCodeTtlMinutes;
        this.passwordResetMaxAttempts = passwordResetMaxAttempts;
        this.passwordResetRequestCooldownSeconds = passwordResetRequestCooldownSeconds;
        this.passwordResetSessionTtlMinutes = passwordResetSessionTtlMinutes;
    }

    @Transactional
    public String register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        if (penggunaRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email sudah terdaftar");
        }
        Pengguna user = new Pengguna();
        user.setNama(request.getNama().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        penggunaRepository.save(user);
        return "Register berhasil";
    }

    @Transactional
    public String login(LoginRequest request) {
        Pengguna user = penggunaRepository.findByEmail(normalizeEmail(request.getEmail()))
                .orElseThrow(() -> new IllegalArgumentException("Email atau password salah"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Email atau password salah");
        }

        return issueToken(user);
    }

    @Transactional
    public String logout(String jwt) {
        Optional<Token> dataToken = tokenRepository.findByToken(jwt);
        dataToken.ifPresent(tokenRepository::delete);
        return "Logout berhasil";
    }

    @Transactional(readOnly = true)
    public boolean isTokenActive(String jwt) {
        if (jwt == null || jwt.isBlank()) {
            return false;
        }
        try {
            String email = jwtUtil.extractEmail(jwt);
            if (email == null || email.isBlank()) {
                return false;
            }
            return tokenRepository.findByToken(jwt).isPresent();
        } catch (Exception ex) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Pengguna getCurrentUser(String jwt) {
        String email = jwtUtil.extractEmail(jwt);
        return penggunaRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Unauthorized"));
    }

    @Transactional
    public Pengguna updateCurrentUser(String jwt, UpdateProfileRequest request) {
        Pengguna user = getCurrentUser(jwt);
        user.setNama(request.getNama().trim());
        user.setLokasi(trimToNull(request.getLokasi()));
        user.setJenisKopi(trimToNull(request.getJenisKopi()));
        user.setNamaAlat(trimToNull(request.getNamaAlat()));
        Pengguna updated = penggunaRepository.save(user);
        notificationService.createForUser(
                updated,
                "PROFILE_UPDATED",
                "Profil diperbarui",
                "Data profil Anda berhasil diperbarui.",
                "INFO"
        );
        return updated;
    }

    @Transactional
    public ForgotPasswordResponse forgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);
        LocalDateTime now = LocalDateTime.now();
        PasswordResetRequest resetRequest = passwordResetRequestRepository.findByEmail(normalizedEmail)
                .orElseGet(PasswordResetRequest::new);

        if (resetRequest.getRequestAvailableAt() != null && now.isBefore(resetRequest.getRequestAvailableAt())) {
            int retryAfterSeconds = (int) java.time.Duration.between(now, resetRequest.getRequestAvailableAt()).getSeconds();
            throw new TooManyRequestsException("Permintaan reset password terlalu sering", Math.max(retryAfterSeconds, 1));
        }

        String verificationCode = generateNumericCode();
        resetRequest.setEmail(normalizedEmail);
        resetRequest.setVerifyAttempts(0);
        resetRequest.setCodeHash(passwordEncoder.encode(verificationCode));
        resetRequest.setExpiresAt(now.plusMinutes(passwordResetCodeTtlMinutes));
        resetRequest.setRequestAvailableAt(now.plusSeconds(passwordResetRequestCooldownSeconds));
        resetRequest.setResetSessionHash(null);
        resetRequest.setResetSessionExpiresAt(null);
        resetRequest.setConsumedAt(null);
        if (resetRequest.getCreatedAt() == null) {
            resetRequest.setCreatedAt(now);
        }
        resetRequest.setUpdatedAt(now);

        penggunaRepository.findByEmail(normalizedEmail).ifPresent(resetRequest::setPengguna);
        passwordResetRequestRepository.save(resetRequest);

        if (resetRequest.getPengguna() != null) {
            passwordResetMailService.sendResetCode(
                    normalizedEmail,
                    resetRequest.getPengguna().getNama(),
                    verificationCode,
                    passwordResetCodeTtlMinutes
            );
        }

        return new ForgotPasswordResponse(FORGOT_PASSWORD_GENERIC_MESSAGE, null);
    }

    @Transactional
    public VerifyResetCodeResponse verifyResetCode(String email, String code) {
        PasswordResetRequest resetRequest = passwordResetRequestRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Kode reset tidak valid"));

        LocalDateTime now = LocalDateTime.now();
        if (resetRequest.getConsumedAt() != null
                || resetRequest.getCodeHash() == null
                || resetRequest.getExpiresAt() == null
                || now.isAfter(resetRequest.getExpiresAt())) {
            throw new IllegalArgumentException("Kode reset sudah kedaluwarsa");
        }
        if (resetRequest.getVerifyAttempts() >= passwordResetMaxAttempts) {
            throw new TooManyRequestsException("Percobaan verifikasi kode reset melebihi batas", passwordResetRequestCooldownSeconds);
        }
        if (!passwordEncoder.matches(code, resetRequest.getCodeHash())) {
            resetRequest.setVerifyAttempts(resetRequest.getVerifyAttempts() + 1);
            resetRequest.setUpdatedAt(now);
            passwordResetRequestRepository.save(resetRequest);
            if (resetRequest.getVerifyAttempts() >= passwordResetMaxAttempts) {
                throw new TooManyRequestsException("Percobaan verifikasi kode reset melebihi batas", passwordResetRequestCooldownSeconds);
            }
            throw new IllegalArgumentException("Kode reset tidak valid");
        }

        String resetSessionToken = generateResetSessionToken();
        resetRequest.setResetSessionHash(passwordEncoder.encode(resetSessionToken));
        resetRequest.setResetSessionExpiresAt(now.plusMinutes(passwordResetSessionTtlMinutes));
        resetRequest.setVerifyAttempts(0);
        resetRequest.setUpdatedAt(now);
        passwordResetRequestRepository.save(resetRequest);
        return new VerifyResetCodeResponse(resetSessionToken, "Kode reset valid");
    }

    @Transactional
    public String resetPassword(String email, String resetSessionToken, String newPassword) {
        PasswordResetRequest resetRequest = passwordResetRequestRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new IllegalArgumentException("Reset password tidak valid"));

        LocalDateTime now = LocalDateTime.now();
        if (resetRequest.getConsumedAt() != null
                || resetRequest.getResetSessionHash() == null
                || resetRequest.getResetSessionExpiresAt() == null
                || now.isAfter(resetRequest.getResetSessionExpiresAt())
                || !passwordEncoder.matches(resetSessionToken, resetRequest.getResetSessionHash())) {
            throw new IllegalArgumentException("Reset password tidak valid");
        }

        Pengguna pengguna = resetRequest.getPengguna();
        if (pengguna == null) {
            throw new IllegalArgumentException("Reset password tidak valid");
        }

        pengguna.setPassword(passwordEncoder.encode(newPassword));
        penggunaRepository.save(pengguna);
        revokeAllTokens(pengguna.getId());

        resetRequest.setConsumedAt(now);
        resetRequest.setCodeHash(null);
        resetRequest.setResetSessionHash(null);
        resetRequest.setResetSessionExpiresAt(null);
        resetRequest.setUpdatedAt(now);
        passwordResetRequestRepository.save(resetRequest);

        notificationService.createForUser(
                pengguna,
                "PASSWORD_RESET",
                "Password diperbarui",
                "Password akun Anda berhasil diperbarui.",
                "INFO"
        );
        return "Password berhasil diperbarui";
    }

    @Transactional
    public void revokeAllTokens(Long penggunaId) {
        tokenRepository.deleteByPenggunaId(penggunaId);
    }

    private String issueToken(Pengguna user) {
        String jwt = jwtUtil.generateToken(user.getEmail());
        Token dataToken = new Token();
        dataToken.setPenggunaId(user.getId());
        dataToken.setToken(jwt);
        dataToken.setCreatedAt(LocalDateTime.now());
        tokenRepository.save(dataToken);
        return jwt;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String generateNumericCode() {
        int bound = (int) Math.pow(10, RESET_CODE_LENGTH);
        int value = SECURE_RANDOM.nextInt(bound);
        return String.format("%0" + RESET_CODE_LENGTH + "d", value);
    }

    private String generateResetSessionToken() {
        byte[] bytes = new byte[RESET_SESSION_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
