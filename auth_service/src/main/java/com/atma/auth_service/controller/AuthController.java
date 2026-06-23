package com.atma.auth_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.atma.auth_service.dto.ForgotPasswordRequest;
import com.atma.auth_service.dto.ForgotPasswordResponse;
import com.atma.auth_service.dto.LoginRequest;
import com.atma.auth_service.dto.ResetPasswordRequest;
import com.atma.auth_service.dto.RegisterRequest;
import com.atma.auth_service.dto.TokenValidationResponse;
import com.atma.auth_service.dto.UpdateProfileRequest;
import com.atma.auth_service.dto.UserResponse;
import com.atma.auth_service.dto.VerifyResetCodeRequest;
import com.atma.auth_service.dto.VerifyResetCodeResponse;
import com.atma.auth_service.model.Pengguna;
import com.atma.auth_service.service.AuthService;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        String result = authService.register(request);
        return ResponseEntity.ok(Map.of("message", result));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request);
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.accepted().body(authService.forgotPassword(request.getEmail()));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<VerifyResetCodeResponse> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        return ResponseEntity.ok(authService.verifyResetCode(request.getEmail(), request.getCode()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(Map.of(
                "message",
                authService.resetPassword(request.getEmail(), request.getResetSessionToken(), request.getNewPassword())
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return ResponseEntity.ok(Map.of("message", authService.logout(token)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Pengguna currentUser = authService.getCurrentUser(token);
        return ResponseEntity.ok(UserResponse.from(currentUser));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        String token = authHeader.replace("Bearer ", "");
        Pengguna currentUser = authService.updateCurrentUser(token, request);
        return ResponseEntity.ok(UserResponse.from(currentUser));
    }

    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        if (!authService.isTokenActive(token)) {
            throw new IllegalArgumentException("Unauthorized");
        }
        Pengguna currentUser = authService.getCurrentUser(token);
        return ResponseEntity.ok(new TokenValidationResponse(true, currentUser.getEmail()));
    }
}
