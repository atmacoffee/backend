package com.atma.auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BroadcastNotificationRequest {

    @NotBlank(message = "Event key wajib diisi")
    @Size(max = 100, message = "Event key maksimal 100 karakter")
    private String eventKey;

    @NotBlank(message = "Judul wajib diisi")
    @Size(max = 150, message = "Judul maksimal 150 karakter")
    private String title;

    @NotBlank(message = "Pesan wajib diisi")
    @Size(max = 500, message = "Pesan maksimal 500 karakter")
    private String message;

    @NotBlank(message = "Severity wajib diisi")
    @Size(max = 32, message = "Severity maksimal 32 karakter")
    private String severity;

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
}
