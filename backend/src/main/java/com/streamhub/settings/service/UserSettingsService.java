package com.streamhub.settings.service;

import com.streamhub.common.entity.User;
import com.streamhub.settings.entity.UserSettings;
import com.streamhub.settings.repository.UserSettingsRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final EntityManager entityManager;

    public UserSettingsService(UserSettingsRepository userSettingsRepository, EntityManager entityManager) {
        this.userSettingsRepository = userSettingsRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public UserSettings getOrCreate(UUID userId) {
        return userSettingsRepository.findById(userId)
                .orElseGet(() -> {
                    User userRef = entityManager.getReference(User.class, userId);
                    UserSettings settings = new UserSettings();
                    settings.setUser(userRef);
                    return userSettingsRepository.save(settings);
                });
    }

    @Transactional
    public UserSettings update(UUID userId, Map<String, Boolean> updates) {
        UserSettings settings = getOrCreate(userId);
        if (updates.containsKey("watchHistoryEnabled")) {
            settings.setWatchHistoryEnabled(updates.get("watchHistoryEnabled"));
        }
        if (updates.containsKey("notifySocial")) {
            settings.setNotifySocial(updates.get("notifySocial"));
        }
        if (updates.containsKey("notifyModeration")) {
            settings.setNotifyModeration(updates.get("notifyModeration"));
        }
        if (updates.containsKey("notifySystem")) {
            settings.setNotifySystem(updates.get("notifySystem"));
        }
        return userSettingsRepository.save(settings);
    }

    // Deletes everything that belongs to this user account, then removes the user row.
    // The order matters — child rows that reference other child rows must go first.
    @Transactional
    public void deleteAccount(UUID userId) {

        // 1. CommentLike rows on replies the user wrote
        entityManager.createNativeQuery(
            "DELETE FROM [CommentLike] WHERE comment_id IN " +
            "(SELECT comment_id FROM [Comment] WHERE user_id = :uid AND parent_comment_id IS NOT NULL)")
            .setParameter("uid", userId).executeUpdate();

        // 2. CommentLike rows on top-level comments the user wrote
        entityManager.createNativeQuery(
            "DELETE FROM [CommentLike] WHERE comment_id IN " +
            "(SELECT comment_id FROM [Comment] WHERE user_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        // 3. CommentLike rows the user created on other people's comments
        entityManager.createNativeQuery(
            "DELETE FROM [CommentLike] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 4. Replies the user wrote (self-referencing FK must go before parent comments)
        entityManager.createNativeQuery(
            "DELETE FROM [Comment] WHERE user_id = :uid AND parent_comment_id IS NOT NULL")
            .setParameter("uid", userId).executeUpdate();

        // 5. All remaining comments by this user
        entityManager.createNativeQuery(
            "DELETE FROM [Comment] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 6. Comments on the user's own videos (other people's comments on their content)
        //    Replies first, then parents
        entityManager.createNativeQuery(
            "DELETE FROM [CommentLike] WHERE comment_id IN " +
            "(SELECT c.comment_id FROM [Comment] c " +
            " INNER JOIN [Video] v ON c.video_id = v.video_id " +
            " WHERE v.uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [Comment] WHERE video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid) " +
            "AND parent_comment_id IS NOT NULL")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [Comment] WHERE video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        // 7. View events and watch history
        entityManager.createNativeQuery(
            "DELETE FROM [VideoViewEvent] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [watch_history] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 8. Playlist videos, then playlists owned by the user
        entityManager.createNativeQuery(
            "DELETE FROM [playlist_video] WHERE playlist_id IN " +
            "(SELECT playlist_id FROM [Playlist] WHERE owner_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        // Also remove any playlist_video rows where the video belongs to the user
        entityManager.createNativeQuery(
            "DELETE FROM [playlist_video] WHERE video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [Playlist] WHERE owner_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 9. Likes and dislikes by this user, and on the user's videos
        entityManager.createNativeQuery(
            "DELETE FROM [Like] WHERE user_id = :uid OR video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [dislike] WHERE user_id = :uid OR video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        // 10. Notifications sent to or triggered by this user
        entityManager.createNativeQuery(
            "DELETE FROM [notification] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 11. Reports filed by or about this user
        entityManager.createNativeQuery(
            "DELETE FROM [report] WHERE reporter_id = :uid OR moderator_id = :uid " +
            "OR (reportable_id IN (SELECT video_id FROM [Video] WHERE uploader_id = :uid))")
            .setParameter("uid", userId).executeUpdate();

        // 12. Videos uploaded by this user (view events already removed above)
        entityManager.createNativeQuery(
            "DELETE FROM [VideoViewEvent] WHERE video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [Video] WHERE uploader_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 13. Follow relationships where this user is follower or following
        entityManager.createNativeQuery(
            "DELETE FROM [Follow] WHERE follower_id = :uid OR following_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 14. Refresh tokens
        entityManager.createNativeQuery(
            "DELETE FROM [refresh_token] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 15. Category preferences and settings
        entityManager.createNativeQuery(
            "DELETE FROM [UserCategoryPreference] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [UserSettings] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        // 16. Finally remove the user account itself
        entityManager.createNativeQuery(
            "DELETE FROM [User] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        entityManager.flush();
    }
}

