package com.streamhub.admin.service;

import com.streamhub.admin.dto.AdminUserDto;
import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.common.exception.ResourceNotFoundException;
import com.streamhub.playlist.entity.Playlist;
import com.streamhub.playlist.repository.PlaylistRepository;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.video.service.VideoService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//business logic for managing user roles, statuses, and full content deletion on ban
public class AdminUserService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final VideoService videoService;
    private final PlaylistRepository playlistRepository;
    private final EntityManager entityManager;
    private final Path avatarStorageLocation;
    private final Path coverStorageLocation;
    private final Path playlistCoverStorageLocation;

    public AdminUserService(UserRepository userRepository) {
        this(userRepository, null, null, null, null, "uploads/avatars", "uploads/covers", "uploads/playlist-covers");
    }

    @Autowired
    public AdminUserService(UserRepository userRepository,
                            VideoRepository videoRepository,
                            VideoService videoService,
                            PlaylistRepository playlistRepository,
                            EntityManager entityManager,
                            @Value("${file.avatar-upload-dir:uploads/avatars}") String avatarUploadDir,
                            @Value("${file.cover-upload-dir:uploads/covers}") String coverUploadDir,
                            @Value("${file.playlist-cover-upload-dir:uploads/playlist-covers}") String playlistCoverUploadDir) {
        this.userRepository = userRepository;
        this.videoRepository = videoRepository;
        this.videoService = videoService;
        this.playlistRepository = playlistRepository;
        this.entityManager = entityManager;
        this.avatarStorageLocation = avatarUploadDir != null ? Paths.get(avatarUploadDir).toAbsolutePath().normalize() : null;
        this.coverStorageLocation = coverUploadDir != null ? Paths.get(coverUploadDir).toAbsolutePath().normalize() : null;
        this.playlistCoverStorageLocation = playlistCoverUploadDir != null ? Paths.get(playlistCoverUploadDir).toAbsolutePath().normalize() : null;
    }

    public List<AdminUserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(AdminUserDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUserRole(UUID userId, String newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        //only allow recognized system roles
        if (!newRole.equals("ADMINISTRATOR") && !newRole.equals("CONTENT_MODERATOR") && !newRole.equals("USER")) {
            throw new IllegalArgumentException("Invalid role");
        }

        user.setRole(newRole);
        userRepository.save(user);
    }

    @Transactional
    public void updateUserStatus(UUID userId, String newStatus) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!newStatus.equals("ACTIVE") && !newStatus.equals("BANNED") && !newStatus.equals("DEACTIVATED")) {
            throw new IllegalArgumentException("Invalid status");
        }

        //if user is banned, purge all user data and stored files from database and filesystem
        if ("BANNED".equals(newStatus)) {
            purgeUserContentAndFiles(user);
        }

        user.setStatus(newStatus);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        purgeUserContentAndFiles(user);
        userRepository.delete(user);
    }

    //purges all videos, physical media files, playlists, comments, likes, history, and relations for banned or deleted user
    @Transactional
    public void purgeUserContentAndFiles(User user) {
        if (user == null) return;
        UUID userId = user.getUserId();

        // 1. Delete all videos uploaded by this user (including physical video files, thumbnails, and video-level relations)
        if (videoRepository != null && videoService != null) {
            List<Video> userVideos = videoRepository.findAllByOwner_UserId(userId);
            for (Video v : userVideos) {
                videoService.deleteVideoInternal(v, "Uploader account banned", null);
            }
        }

        // 2. Delete all playlists owned by this user (and physical playlist cover images)
        if (playlistRepository != null) {
            List<Playlist> playlists = playlistRepository.findByOwnerOrderByCreatedAtDesc(user);
            for (Playlist p : playlists) {
                String thumbUrl = p.getThumbnailUrl();
                if (thumbUrl != null && thumbUrl.contains("/uploads/playlist-covers/") && this.playlistCoverStorageLocation != null) {
                    String filename = thumbUrl.substring(thumbUrl.lastIndexOf('/') + 1);
                    try {
                        Files.deleteIfExists(this.playlistCoverStorageLocation.resolve(filename));
                    } catch (Exception ignored) {}
                }
                if (entityManager != null) {
                    entityManager.createNativeQuery("DELETE FROM playlist_video WHERE playlist_id = :pid")
                            .setParameter("pid", p.getPlaylistId()).executeUpdate();
                    entityManager.createNativeQuery("DELETE FROM report WHERE reportable_type = 'PLAYLIST' AND reportable_id = :pid")
                            .setParameter("pid", p.getPlaylistId()).executeUpdate();
                }
                playlistRepository.delete(p);
            }
        }

        // 3. Delete physical profile files (avatar and cover banner images) from disk
        if (user.getAvatarUrl() != null && this.avatarStorageLocation != null) {
            String avatarUrl = user.getAvatarUrl();
            if (avatarUrl.contains("/avatar/")) {
                String filename = avatarUrl.substring(avatarUrl.lastIndexOf('/') + 1);
                try {
                    Files.deleteIfExists(this.avatarStorageLocation.resolve(filename));
                } catch (Exception ignored) {}
            }
        }
        if (user.getCoverImageUrl() != null && this.coverStorageLocation != null) {
            String coverUrl = user.getCoverImageUrl();
            if (coverUrl.contains("/cover/")) {
                String filename = coverUrl.substring(coverUrl.lastIndexOf('/') + 1);
                try {
                    Files.deleteIfExists(this.coverStorageLocation.resolve(filename));
                } catch (Exception ignored) {}
            }
        }
        user.setAvatarUrl(null);
        user.setCoverImageUrl(null);
        user.setBio(null);

        // 4. Delete dependent user data across all tables
        if (entityManager != null) {
            // Delete comment likes on user's comments
            entityManager.createNativeQuery(
                    "DELETE FROM [CommentLike] WHERE comment_id IN (SELECT comment_id FROM [Comment] WHERE user_id = :uid)")
                    .setParameter("uid", userId).executeUpdate();

            // Delete child replies to user's comments
            entityManager.createNativeQuery(
                    "DELETE FROM [CommentLike] WHERE comment_id IN (SELECT comment_id FROM [Comment] WHERE parent_comment_id IN (SELECT comment_id FROM [Comment] WHERE user_id = :uid))")
                    .setParameter("uid", userId).executeUpdate();
            entityManager.createNativeQuery(
                    "DELETE FROM [Comment] WHERE parent_comment_id IN (SELECT comment_id FROM [Comment] WHERE user_id = :uid)")
                    .setParameter("uid", userId).executeUpdate();

            // Delete reports on user's comments
            entityManager.createNativeQuery(
                    "DELETE FROM report WHERE reportable_type = 'COMMENT' AND reportable_id IN (SELECT comment_id FROM [Comment] WHERE user_id = :uid)")
                    .setParameter("uid", userId).executeUpdate();

            // Delete user's comments
            entityManager.createNativeQuery("DELETE FROM [Comment] WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();

            // Delete user's likes, dislikes, and comment likes on other content
            entityManager.createNativeQuery("DELETE FROM [Like] WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM [dislike] WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM [CommentLike] WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();

            // Delete watch history and video view events
            entityManager.createNativeQuery("DELETE FROM watch_history WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM VideoViewEvent WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();

            // Delete follows (both follower and following)
            entityManager.createNativeQuery("DELETE FROM Follow WHERE follower_id = :uid OR following_id = :uid")
                    .setParameter("uid", userId).executeUpdate();

            // Delete category preferences and settings
            entityManager.createNativeQuery("DELETE FROM UserCategoryPreference WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM UserSettings WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();

            // Delete tokens and notifications
            entityManager.createNativeQuery("DELETE FROM refresh_token WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM notification WHERE user_id = :uid")
                    .setParameter("uid", userId).executeUpdate();

            // Delete reports filed by user or against user profile
            entityManager.createNativeQuery("DELETE FROM report WHERE reporter_id = :uid")
                    .setParameter("uid", userId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM report WHERE reportable_type = 'PROFILE' AND reportable_id = :uid")
                    .setParameter("uid", userId).executeUpdate();
            entityManager.createNativeQuery("UPDATE report SET moderator_id = NULL WHERE moderator_id = :uid")
                    .setParameter("uid", userId).executeUpdate();

            entityManager.flush();
        }
    }
}
