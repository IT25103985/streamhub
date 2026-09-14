package com.streamhub.notification.service;

import com.streamhub.common.entity.User;
import com.streamhub.common.exception.ResourceNotFoundException;
import com.streamhub.notification.dto.NotificationResponse;
import com.streamhub.notification.entity.Notification;
import com.streamhub.notification.repository.NotificationRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
//creates and manages user notifications and read statuses
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EntityManager entityManager;

    public NotificationService(NotificationRepository notificationRepository, EntityManager entityManager) {
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public void createNotification(UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId) {
        //use entity reference proxy to avoid extra db query
        User userRef = entityManager.getReference(User.class, userId);
        Notification notification = new Notification(userRef, type, title, message, relatedEntityType, relatedEntityId);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getUserId().equals(userId)) {
            //treat mismatch as not found to avoid leaking notification existence
            throw new ResourceNotFoundException("Notification not found");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getRelatedEntityType(),
                notification.getRelatedEntityId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
