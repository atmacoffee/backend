CREATE TABLE password_reset_request (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pengguna_id BIGINT NULL,
    email VARCHAR(255) NOT NULL,
    code_hash VARCHAR(255) NULL,
    expires_at DATETIME NULL,
    verify_attempts INT NOT NULL DEFAULT 0,
    request_available_at DATETIME NOT NULL,
    reset_session_hash VARCHAR(255) NULL,
    reset_session_expires_at DATETIME NULL,
    consumed_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_password_reset_request_email UNIQUE (email),
    CONSTRAINT fk_password_reset_request_pengguna
        FOREIGN KEY (pengguna_id) REFERENCES pengguna(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_password_reset_request_pengguna_id
    ON password_reset_request (pengguna_id);
CREATE INDEX idx_password_reset_request_expires_at
    ON password_reset_request (expires_at);

CREATE TABLE notification (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    pengguna_id BIGINT NOT NULL,
    event_key VARCHAR(100) NOT NULL,
    title VARCHAR(150) NOT NULL,
    message VARCHAR(500) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    CONSTRAINT fk_notification_pengguna
        FOREIGN KEY (pengguna_id) REFERENCES pengguna(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_notification_pengguna_created_at
    ON notification (pengguna_id, created_at DESC);
CREATE INDEX idx_notification_pengguna_is_read
    ON notification (pengguna_id, is_read);
