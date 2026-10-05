package com.streamhub.social.event;

import com.streamhub.notification.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer / Listener that handles notification dispatching for social interactions
 * (video likes, comment likes, video comments, and comment replies).
 */
@Component
public class SocialNotificationListener {

    private final NotificationService notificationService;

    public SocialNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Handles video liked events.
     * 
     * - AFTER_COMMIT: ensures no notification is sent for a transaction that rolls back.
     * - fallbackExecution: ensures the event is not silently dropped if the publisher is invoked outside an active transaction.
     * - REQUIRES_NEW: database writes made after the original transaction has committed must run in a new active transaction to be committed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onVideoLiked(VideoLikedEvent event) {
        if (notificationService == null || event == null) {
            return;
        }

        try {
            String title = "New Like on Your Video";
            String message = event.actorUsername() + " liked your video \"" + event.videoTitle() + "\".";

            notificationService.createNotification(
                    event.recipientUserId(),
                    "LIKE",
                    title,
                    message,
                    "VIDEO",
                    event.videoId()
            );
        } catch (Exception e) {
            System.err.println("Failed to send like notification: " + e.getMessage());
        }
    }

    /**
     * Handles comment liked events.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentLiked(CommentLikedEvent event) {
        if (notificationService == null || event == null) {
            return;
        }

        try {
            String title = "New Like on Your Comment";
            String message = event.actorUsername() + " liked your comment on \"" + event.videoTitle() + "\".";

            notificationService.createNotification(
                    event.recipientUserId(),
                    "COMMENT_LIKE",
                    title,
                    message,
                    "COMMENT",
                    event.commentId()
            );
        } catch (Exception e) {
            System.err.println("Failed to send comment like notification: " + e.getMessage());
        }
    }

    /**
     * Handles new comment posted on video events.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentCreated(CommentCreatedEvent event) {
        if (notificationService == null || event == null) {
            return;
        }

        try {
            String title = "New Comment on Your Video";
            String message = event.actorUsername() + " commented on your video \"" + event.videoTitle() + "\": \"" + event.commentPreview() + "\"";

            notificationService.createNotification(
                    event.recipientUserId(),
                    "COMMENT",
                    title,
                    message,
                    "VIDEO",
                    event.videoId()
            );
        } catch (Exception e) {
            System.err.println("Failed to send comment notification: " + e.getMessage());
        }
    }

    /**
     * Handles new reply posted to existing comment events.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCommentReplied(CommentRepliedEvent event) {
        if (notificationService == null || event == null) {
            return;
        }

        try {
            String title = "New Reply to Your Comment";
            String message = event.actorUsername() + " replied to your comment on \"" + event.videoTitle() + "\": \"" + event.commentPreview() + "\"";

            notificationService.createNotification(
                    event.recipientUserId(),
                    "COMMENT_REPLY",
                    title,
                    message,
                    "VIDEO",
                    event.videoId()
            );
        } catch (Exception e) {
            System.err.println("Failed to send comment reply notification: " + e.getMessage());
        }
    }
}
