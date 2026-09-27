package com.streamhub.notification.service;

import com.streamhub.common.entity.User;
import com.streamhub.common.exception.ResourceNotFoundException;
import com.streamhub.notification.dto.NotificationResponse;
import com.streamhub.notification.entity.Notification;
import com.streamhub.notification.repository.NotificationRepository;
import com.streamhub.settings.entity.UserSettings;
import com.streamhub.settings.service.UserSettingsService;
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
    private final UserSettingsService userSettingsService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    public NotificationService(NotificationRepository notificationRepository, EntityManager entityManager, 
                               @org.springframework.context.annotation.Lazy UserSettingsService userSettingsService,
                               @org.springframework.beans.factory.annotation.Autowired(required = false) org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate) {
        this.notificationRepository = notificationRepository;
        this.entityManager = entityManager;
        this.userSettingsService = userSettingsService;
        this.messagingTemplate = messagingTemplate;
    }

    //creates and persists notification and broadcasts via websocket if enabled by user preferences
    @Transactional
    public NotificationResponse createNotification(UUID userId, String type, String title, String message, String relatedEntityType, UUID relatedEntityId) {
        if (userId == null) return null;
        try {
            UserSettings settings = userSettingsService.getOrCreate(userId);
            boolean shouldCreate = true;

            switch(type) {
                case "LIKE":
                case "COMMENT_LIKE":
                case "COMMENT":
                case "COMMENT_REPLY":
                case "FOLLOW":
                case "PLAYLIST_ADD":
                case "VIDEO_UPLOAD":
                case "VIDEO_DELETE":
                    shouldCreate = settings.isNotifySocial();
                    break;
                case "MODERATION":
                case "MODERATION_ACTION":
                case "REPORT_SUBMITTED":
                case "REPORT_STATUS_UPDATE":
                case "REPORT_RESOLVED":
                case "REPORT_UPDATE":
                case "REPORT_ACTION":
                    shouldCreate = settings.isNotifyModeration();
                    break;
                case "SYSTEM_ALERT":
                default:
                    shouldCreate = settings.isNotifySystem();
                    break;
            }

            if (!shouldCreate) {
                return null;
            }

            //use entity reference proxy to avoid extra db query
            User userRef = entityManager.getReference(User.class, userId);
            String safeTitle = title != null ? (title.length() > 140 ? title.substring(0, 137) + "..." : title) : "Notification";
            String safeMessage = message != null ? (message.length() > 480 ? message.substring(0, 477) + "..." : message) : "";
            Notification notification = new Notification(userRef, type, safeTitle, safeMessage, relatedEntityType, relatedEntityId);
            Notification saved = notificationRepository.save(notification);
            
            NotificationResponse response = mapToResponse(saved);
            
            if (messagingTemplate != null && userRef.getUsername() != null) {
                try {
                    messagingTemplate.convertAndSendToUser(
                            userRef.getUsername(),
                            "/queue/notifications",
                            response
                    );
                } catch (Exception e) {
                    System.err.println("Failed to push STOMP notification: " + e.getMessage());
                }
            }
            
            return response;
        } catch (Exception e) {
            System.err.println("Failed to create notification for user " + userId + ": " + e.getMessage());
            return null;
        }
    }

    //retrieves paginated list of notifications for user
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUser_UserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    //counts unread notifications for user badge
    @Transactional(readOnly = true)
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUser_UserIdAndIsReadFalse(userId);
    }

    //marks single notification as read verifying recipient ownership
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

    //marks all notifications as read for specified user
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
