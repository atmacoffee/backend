package com.atma.auth_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.atma.auth_service.dto.ForgotPasswordResponse;
import com.atma.auth_service.dto.LoginRequest;
import com.atma.auth_service.dto.RegisterRequest;
import com.atma.auth_service.dto.UpdateProfileRequest;
import com.atma.auth_service.dto.VerifyResetCodeResponse;
import com.atma.auth_service.exception.TooManyRequestsException;
import com.atma.auth_service.model.PasswordResetRequest;
import com.atma.auth_service.model.Pengguna;
import com.atma.auth_service.repository.PasswordResetRequestRepository;
import com.atma.auth_service.repository.PenggunaRepository;
import com.atma.auth_service.repository.TokenRepository;
import com.atma.auth_service.security.JwtUtil;

class AuthServiceTest {

    @Mock
    private PenggunaRepository penggunaRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private PasswordResetRequestRepository passwordResetRequestRepository;

    @Mock
    private PasswordResetMailService passwordResetMailService;

    @Mock
    private NotificationService notificationService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(
                penggunaRepository,
                passwordEncoder,
                jwtUtil,
                tokenRepository,
                passwordResetRequestRepository,
                passwordResetMailService,
                notificationService,
                10,
                3,
                60,
                10
        );
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setNama("ATMA");
        request.setEmail("user@example.com");
        request.setPassword("Password123");
        when(penggunaRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(new Pengguna()));

        assertThrows(IllegalArgumentException.class, () -> authService.register(request));
    }

    @Test
    void loginStoresTokenWhenCredentialsValid() {
        Pengguna pengguna = pengguna("user@example.com");
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Password123");
        when(penggunaRepository.findByEmail("user@example.com")).thenReturn(Optional.of(pengguna));
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(true);
        when(jwtUtil.generateToken("user@example.com")).thenReturn("jwt-token");

        String token = authService.login(request);

        assertEquals("jwt-token", token);
        verify(tokenRepository).save(any());
    }

    @Test
    void forgotPasswordUsesGenericResponseForUnknownEmail() {
        when(passwordResetRequestRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(penggunaRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-code");

        ForgotPasswordResponse response = authService.forgotPassword("ghost@example.com");

        assertTrue(response.message().contains("Jika email terdaftar"));
        verify(passwordResetMailService, never()).sendResetCode(any(), any(), any(), any(Integer.class));
    }

    @Test
    void forgotPasswordRejectsCooldownWindow() {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("user@example.com");
        request.setRequestAvailableAt(LocalDateTime.now().plusSeconds(30));
        when(passwordResetRequestRepository.findByEmail("user@example.com")).thenReturn(Optional.of(request));

        assertThrows(TooManyRequestsException.class, () -> authService.forgotPassword("user@example.com"));
    }

    @Test
    void verifyAndResetPasswordRevokesAllTokens() {
        Pengguna pengguna = pengguna("user@example.com");
        PasswordResetRequest request = new PasswordResetRequest();
        request.setPengguna(pengguna);
        request.setEmail("user@example.com");
        request.setCodeHash("encoded-code");
        request.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        when(passwordResetRequestRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(request));
        when(passwordEncoder.matches("123456", "encoded-code")).thenReturn(true);
        when(passwordEncoder.encode("new-session-token")).thenReturn("encoded-session-token");

        VerifyResetCodeResponse verifyResponse = authService.verifyResetCode("user@example.com", "123456");

        request.setResetSessionHash("encoded-session-token");
        request.setResetSessionExpiresAt(LocalDateTime.now().plusMinutes(10));
        when(passwordResetRequestRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(request));
        when(passwordEncoder.matches(verifyResponse.resetSessionToken(), "encoded-session-token")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword123")).thenReturn("new-password-hash");

        String resetMessage = authService.resetPassword(
                "user@example.com",
                verifyResponse.resetSessionToken(),
                "NewPassword123"
        );

        assertEquals("Password berhasil diperbarui", resetMessage);
        verify(tokenRepository).deleteByPenggunaId(pengguna.getId());
    }

    @Test
    void updateProfilePersistsSanitizedFields() {
        Pengguna pengguna = pengguna("user@example.com");
        when(penggunaRepository.findByEmail("user@example.com")).thenReturn(Optional.of(pengguna));
        when(jwtUtil.extractEmail("jwt-token")).thenReturn("user@example.com");
        when(penggunaRepository.save(any(Pengguna.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNama(" ATMA User ");
        request.setLokasi(" Sleman ");
        request.setJenisKopi(" Arabika ");
        request.setNamaAlat(" ATMA-02 ");

        Pengguna updated = authService.updateCurrentUser("jwt-token", request);

        assertEquals("ATMA User", updated.getNama());
        assertEquals("Sleman", updated.getLokasi());
        verify(notificationService).createForUser(any(), any(), any(), any(), any());
    }

    @Test
    void invalidTokenIsNotActive() {
        when(jwtUtil.extractEmail("bad-token")).thenThrow(new IllegalArgumentException("bad-token"));
        assertFalse(authService.isTokenActive("bad-token"));
    }

    private Pengguna pengguna(String email) {
        Pengguna pengguna = new Pengguna();
        pengguna.setId(1L);
        pengguna.setNama("ATMA");
        pengguna.setEmail(email);
        pengguna.setPassword("hashed-password");
        return pengguna;
    }
}
