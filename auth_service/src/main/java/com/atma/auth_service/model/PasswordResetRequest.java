package com.atma.auth_service.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "password_reset_request")
public class PasswordResetRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pengguna_id")
    private Pengguna pengguna;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "code_hash")
    private String codeHash;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "verify_attempts", nullable = false)
    private int verifyAttempts;

    @Column(name = "request_available_at", nullable = false)
    private LocalDateTime requestAvailableAt;

    @Column(name = "reset_session_hash")
    private String resetSessionHash;

    @Column(name = "reset_session_expires_at")
    private LocalDateTime resetSessionExpiresAt;

    @Column(name = "consumed_at")
    private LocalDateTime consumedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Pengguna getPengguna() {
        return pengguna;
    }

    public void setPengguna(Pengguna pengguna) {
        this.pengguna = pengguna;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getVerifyAttempts() {
        return verifyAttempts;
    }

    public void setVerifyAttempts(int verifyAttempts) {
        this.verifyAttempts = verifyAttempts;
    }

    public LocalDateTime getRequestAvailableAt() {
        return requestAvailableAt;
    }

    public void setRequestAvailableAt(LocalDateTime requestAvailableAt) {
        this.requestAvailableAt = requestAvailableAt;
    }

    public String getResetSessionHash() {
        return resetSessionHash;
    }

    public void setResetSessionHash(String resetSessionHash) {
        this.resetSessionHash = resetSessionHash;
    }

    public LocalDateTime getResetSessionExpiresAt() {
        return resetSessionExpiresAt;
    }

    public void setResetSessionExpiresAt(LocalDateTime resetSessionExpiresAt) {
        this.resetSessionExpiresAt = resetSessionExpiresAt;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
