package com.atma.auth_service.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpPasswordResetMailService implements PasswordResetMailService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SmtpPasswordResetMailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String fromName;

    public SmtpPasswordResetMailService(
            JavaMailSender mailSender,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.from-name}") String fromName
    ) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
    }

    @Override
    public void sendResetCode(String recipientEmail, String recipientName, String code, int ttlMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setFrom(fromAddress);
        message.setSubject("Kode reset password ATMA Dryer");
        message.setText("""
                Halo %s,

                Gunakan kode berikut untuk reset password ATMA Dryer:
                %s

                Kode ini berlaku selama %d menit.
                Jika Anda tidak meminta reset password, abaikan email ini.

                %s
                """.formatted(recipientName == null || recipientName.isBlank() ? "Pengguna" : recipientName, code, ttlMinutes, fromName));
        mailSender.send(message);
        LOGGER.info("Password reset email queued for {}", recipientEmail);
    }
}
