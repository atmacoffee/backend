package com.atma.auth_service.service;

public interface PasswordResetMailService {

    void sendResetCode(String recipientEmail, String recipientName, String code, int ttlMinutes);
}
