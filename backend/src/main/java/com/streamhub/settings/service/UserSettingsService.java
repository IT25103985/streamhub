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
//service handling retrieval modification of user preferences and account deletion
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final EntityManager entityManager;

    public UserSettingsService(UserSettingsRepository userSettingsRepository, EntityManager entityManager) {
        this.userSettingsRepository = userSettingsRepository;
        this.entityManager = entityManager;
    }

    //retrieves existing user settings or creates default settings entry
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

    //updates individual notification and history preferences
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

    //cascades deletion across all related entities before removing user record
    @Transactional
    public void deleteAccount(UUID userId) {

        //deletes comment likes on replies authored by user
        entityManager.createNativeQuery(
            "DELETE FROM [CommentLike] WHERE comment_id IN " +
            "(SELECT comment_id FROM [Comment] WHERE user_id = :uid AND parent_comment_id IS NOT NULL)")
            .setParameter("uid", userId).executeUpdate();

        //deletes comment likes on comments authored by user
        entityManager.createNativeQuery(
            "DELETE FROM [CommentLike] WHERE comment_id IN " +
            "(SELECT comment_id FROM [Comment] WHERE user_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        //deletes comment likes given by user
        entityManager.createNativeQuery(
            "DELETE FROM [CommentLike] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes reply comments authored by user
        entityManager.createNativeQuery(
            "DELETE FROM [Comment] WHERE user_id = :uid AND parent_comment_id IS NOT NULL")
            .setParameter("uid", userId).executeUpdate();

        //deletes top level comments authored by user
        entityManager.createNativeQuery(
            "DELETE FROM [Comment] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes comments on videos uploaded by user
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

        //deletes view events and watch history records
        entityManager.createNativeQuery(
            "DELETE FROM [VideoViewEvent] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [watch_history] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes playlist items and playlists owned by user
        entityManager.createNativeQuery(
            "DELETE FROM [playlist_video] WHERE playlist_id IN " +
            "(SELECT playlist_id FROM [Playlist] WHERE owner_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        //removes playlist items referencing videos of user
        entityManager.createNativeQuery(
            "DELETE FROM [playlist_video] WHERE video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [Playlist] WHERE owner_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes video likes and dislikes
        entityManager.createNativeQuery(
            "DELETE FROM [Like] WHERE user_id = :uid OR video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [dislike] WHERE user_id = :uid OR video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        //deletes notifications associated with user
        entityManager.createNativeQuery(
            "DELETE FROM [notification] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes reports filed by or targeting user
        entityManager.createNativeQuery(
            "DELETE FROM [report] WHERE reporter_id = :uid OR moderator_id = :uid " +
            "OR (reportable_id IN (SELECT video_id FROM [Video] WHERE uploader_id = :uid))")
            .setParameter("uid", userId).executeUpdate();

        //deletes uploaded videos and associated views
        entityManager.createNativeQuery(
            "DELETE FROM [VideoViewEvent] WHERE video_id IN " +
            "(SELECT video_id FROM [Video] WHERE uploader_id = :uid)")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [Video] WHERE uploader_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes follow relationships for user
        entityManager.createNativeQuery(
            "DELETE FROM [Follow] WHERE follower_id = :uid OR following_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes refresh tokens of user
        entityManager.createNativeQuery(
            "DELETE FROM [refresh_token] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes category preferences and settings
        entityManager.createNativeQuery(
            "DELETE FROM [UserCategoryPreference] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        entityManager.createNativeQuery(
            "DELETE FROM [UserSettings] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        //deletes user account record
        entityManager.createNativeQuery(
            "DELETE FROM [User] WHERE user_id = :uid")
            .setParameter("uid", userId).executeUpdate();

        entityManager.flush();
    }
}

