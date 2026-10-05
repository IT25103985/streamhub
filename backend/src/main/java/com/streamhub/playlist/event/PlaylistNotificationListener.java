package com.streamhub.playlist.event;

import com.streamhub.notification.service.NotificationService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Observer / Listener that handles notification dispatching when a video is added to a public playlist.
 */
@Component
public class PlaylistNotificationListener {

    private final NotificationService notificationService;

    public PlaylistNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Handles playlist video addition events.
     * 
     * - AFTER_COMMIT: ensures no notification is sent for a transaction that rolls back.
     * - fallbackExecution: ensures the event is not silently dropped if the publisher is invoked outside an active transaction.
     * - REQUIRES_NEW: database writes made after the original transaction has committed must run in a new active transaction to be committed.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onPlaylistVideoAdded(PlaylistVideoAddedEvent event) {
        if (notificationService == null || event == null) {
            return;
        }

        try {
            String title = "Video Added to Public Playlist";
            String message = event.actorUsername() + " added your video \"" + event.videoTitle() + "\" to the public playlist \"" + event.playlistName() + "\".";
            
            notificationService.createNotification(
                    event.recipientUserId(),
                    "PLAYLIST_ADD",
                    title,
                    message,
                    "PLAYLIST",
                    event.playlistId()
            );
        } catch (Exception e) {
            System.err.println("Failed to send playlist notification: " + e.getMessage());
        }
    }
}
