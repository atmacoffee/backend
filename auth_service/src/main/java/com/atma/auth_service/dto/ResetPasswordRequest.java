package com.atma.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {

    @NotBlank(message = "Email wajib diisi")
    @Email(message = "Format email tidak valid")
    private String email;

    @NotBlank(message = "Reset session token wajib diisi")
    private String resetSessionToken;

    @NotBlank(message = "Password baru wajib diisi")
    @Size(min = 8, message = "Password minimal 8 karakter")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password harus mengandung huruf dan angka"
    )
    private String newPassword;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getResetSessionToken() {
        return resetSessionToken;
    }

    public void setResetSessionToken(String resetSessionToken) {
        this.resetSessionToken = resetSessionToken;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
