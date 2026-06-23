package com.atma.auth_service.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atma.auth_service.dto.BroadcastNotificationRequest;
import com.atma.auth_service.dto.NotificationResponse;
import com.atma.auth_service.exception.ForbiddenException;
import com.atma.auth_service.model.Notification;
import com.atma.auth_service.model.Pengguna;
import com.atma.auth_service.repository.NotificationRepository;
import com.atma.auth_service.repository.PenggunaRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PenggunaRepository penggunaRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            PenggunaRepository penggunaRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.penggunaRepository = penggunaRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pengguna pengguna, Pageable pageable) {
        return notificationRepository.findByPenggunaIdOrderByCreatedAtDesc(pengguna.getId(), pageable)
                .map(NotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Pengguna pengguna) {
        return notificationRepository.countByPenggunaIdAndReadFalse(pengguna.getId());
    }

    @Transactional
    public NotificationResponse markRead(Pengguna pengguna, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notifikasi tidak ditemukan"));
        if (!notification.getPengguna().getId().equals(pengguna.getId())) {
            throw new ForbiddenException("Notifikasi bukan milik pengguna ini");
        }
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
        }
        return NotificationResponse.from(notificationRepository.save(notification));
    }

    @Transactional
    public int markAllRead(Pengguna pengguna) {
        return notificationRepository.markAllRead(pengguna.getId());
    }

    @Transactional
    public void broadcast(BroadcastNotificationRequest request) {
        List<Pengguna> users = penggunaRepository.findAll();
        LocalDateTime createdAt = LocalDateTime.now();
        for (Pengguna user : users) {
            Notification notification = new Notification();
            notification.setPengguna(user);
            notification.setEventKey(request.getEventKey().trim());
            notification.setTitle(request.getTitle().trim());
            notification.setMessage(request.getMessage().trim());
            notification.setSeverity(request.getSeverity().trim().toUpperCase());
            notification.setRead(false);
            notification.setCreatedAt(createdAt);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void createForUser(Pengguna pengguna, String eventKey, String title, String message, String severity) {
        Notification notification = new Notification();
        notification.setPengguna(pengguna);
        notification.setEventKey(eventKey);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setSeverity(severity.toUpperCase());
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }
}
