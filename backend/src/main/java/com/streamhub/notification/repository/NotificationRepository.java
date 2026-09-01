package com.streamhub.notification.repository;

import com.streamhub.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
//repository querying notifications by user id with unread filter
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUser_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUser_UserIdAndIsReadFalse(UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :userId AND n.isRead = false")
    void markAllAsReadByUserId(@Param("userId") UUID userId);
}
