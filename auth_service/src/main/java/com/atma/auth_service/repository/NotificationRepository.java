package com.atma.auth_service.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.atma.auth_service.model.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByPenggunaIdOrderByCreatedAtDesc(Long penggunaId, Pageable pageable);

    long countByPenggunaIdAndReadFalse(Long penggunaId);

    @Modifying
    @Query("""
            update Notification n
            set n.read = true, n.readAt = CURRENT_TIMESTAMP
            where n.pengguna.id = :penggunaId and n.read = false
            """)
    int markAllRead(Long penggunaId);
}
