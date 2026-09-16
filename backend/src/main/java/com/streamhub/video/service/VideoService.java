package com.streamhub.video.service;

import com.streamhub.common.entity.User;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.entity.Video;
import com.streamhub.category.repository.CategoryRepository;
import com.streamhub.category.entity.Category;
import com.streamhub.category.dto.CategoryDto;
import com.streamhub.video.repository.VideoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//handles video file storage range streaming view counts and moderation deletion
public class VideoService {

    private final VideoRepository videoRepository;
    private final CategoryRepository categoryRepository;
    private final Path fileStorageLocation;
    private final Path thumbnailStorageLocation;
    private final jakarta.persistence.EntityManager entityManager;
    private final com.streamhub.notification.service.NotificationService notificationService;
    private final com.streamhub.watchhistory.repository.VideoViewEventRepository videoViewEventRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public VideoService(VideoRepository videoRepository, 
                        CategoryRepository categoryRepository, 
                        @Value("${file.upload-dir}") String uploadDir, 
                        jakarta.persistence.EntityManager entityManager,
                        com.streamhub.notification.service.NotificationService notificationService,
                        com.streamhub.watchhistory.repository.VideoViewEventRepository videoViewEventRepository) {
        this.videoRepository = videoRepository;
        this.categoryRepository = categoryRepository;
        this.entityManager = entityManager;
        this.notificationService = notificationService;
        this.videoViewEventRepository = videoViewEventRepository;
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.thumbnailStorageLocation = Paths.get(uploadDir, "thumbnails").toAbsolutePath().normalize();
        
        try {
            Files.createDirectories(this.fileStorageLocation);
            Files.createDirectories(this.thumbnailStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the directory where the uploaded files will be stored.", ex);
        }
    }

    public VideoService(VideoRepository videoRepository, 
                        CategoryRepository categoryRepository, 
                        String uploadDir, 
                        jakarta.persistence.EntityManager entityManager,
                        com.streamhub.notification.service.NotificationService notificationService) {
        this(videoRepository, categoryRepository, uploadDir, entityManager, notificationService, null);
    }

    //central access check enforcing video privacy and moderation visibility
    public boolean canUserAccessVideo(Video video, User currentUser) {
        if (video == null) {
            return false;
        }
        UUID currentUserId = currentUser != null ? currentUser.getUserId() : null;
        String currentUserRole = currentUser != null ? currentUser.getRole() : null;
        return canUserAccessVideo(video, currentUserId, currentUserRole);
    }

    //overloaded access check for user id and role strings
    public boolean canUserAccessVideo(Video video, UUID currentUserId, String currentUserRole) {
        if (video == null) {
            return false;
        }
        boolean isOwner = currentUserId != null && video.getOwner() != null && currentUserId.equals(video.getOwner().getUserId());
        boolean isAdminOrMod = "ADMINISTRATOR".equals(currentUserRole) || "CONTENT_MODERATOR".equals(currentUserRole);

        if (video.isHidden()) {
            if (!isOwner && !isAdminOrMod) {
                return false;
            }
        }

        if ("PRIVATE".equalsIgnoreCase(video.getPrivacy())) {
            if (!isOwner && !isAdminOrMod) {
                return false;
            }
        }

        return true;
    }

    //video file storage and thumbnail processing logic
    public VideoResponseDTO uploadVideo(String title, String description, UUID categoryId, MultipartFile file, MultipartFile thumbnail, User owner) {
        return uploadVideo(title, description, categoryId, file, thumbnail, owner, "PUBLIC", null);
    }

    public VideoResponseDTO uploadVideo(String title, String description, UUID categoryId, MultipartFile file, MultipartFile thumbnail, User owner, String privacy) {
        return uploadVideo(title, description, categoryId, file, thumbnail, owner, privacy, null);
    }

    public VideoResponseDTO uploadVideo(String title, String description, UUID categoryId, MultipartFile file, MultipartFile thumbnail, User owner, String privacy, Integer durationSeconds) {
        if (file.isEmpty() || thumbnail.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty file or thumbnail");
        }

        if (file.getSize() > 100 * 1024 * 1024) { //100mb max file size
            throw new IllegalArgumentException("Video file size exceeds 100MB limit");
        }

        if (thumbnail.getSize() > 2 * 1024 * 1024) { //2mb max file size
            throw new IllegalArgumentException("Thumbnail file size exceeds 2MB limit");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("video/")) {
            throw new IllegalArgumentException("Invalid file type. Only video files are allowed");
        }

        String thumbContentType = thumbnail.getContentType();
        if (thumbContentType == null || (!thumbContentType.equals("image/jpeg") && !thumbContentType.equals("image/png") && !thumbContentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Invalid thumbnail type. Only jpg, png, and webp are allowed");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        
        String thumbOriginalFilename = thumbnail.getOriginalFilename();
        String thumbExtension = "";
        if (thumbOriginalFilename != null && thumbOriginalFilename.contains(".")) {
            thumbExtension = thumbOriginalFilename.substring(thumbOriginalFilename.lastIndexOf("."));
        }
        String thumbNewFilename = UUID.randomUUID().toString() + thumbExtension;
        String thumbRelativePath = "/uploads/thumbnails/" + thumbNewFilename;
        
        Path targetLocation = this.fileStorageLocation.resolve(newFilename);
        Path thumbTargetLocation = this.thumbnailStorageLocation.resolve(thumbNewFilename);
        
        try {
            Files.copy(file.getInputStream(), targetLocation);
            
            try {
                Files.copy(thumbnail.getInputStream(), thumbTargetLocation);
            } catch (IOException e) {
                Files.deleteIfExists(targetLocation);
                throw new RuntimeException("Could not store thumbnail file. Video upload rolled back.", e);
            }
            
            String effectivePrivacy = (privacy != null && "PRIVATE".equalsIgnoreCase(privacy.trim())) ? "PRIVATE" : "PUBLIC";
            Video video = new Video(title, description, newFilename, thumbRelativePath, owner, effectivePrivacy);
            video.setDurationSeconds(durationSeconds);
            
            if (categoryId != null) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new RuntimeException("Category not found"));
                video.setCategory(category);
            }

            Video savedVideo = videoRepository.save(video);
            
            if (owner != null && notificationService != null) {
                try {
                    notificationService.createNotification(
                        owner.getUserId(),
                        "VIDEO_UPLOAD",
                        "Video Uploaded Successfully",
                        "Your video \"" + savedVideo.getTitle() + "\" has been successfully uploaded and is now live.",
                        "VIDEO",
                        savedVideo.getVideoId()
                    );
                } catch (Exception notifEx) {
                    System.err.println("Failed to send video upload notification: " + notifEx.getMessage());
                }
            }

            return mapToDTO(savedVideo);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file. Please try again!", ex);
        }
    }

    public VideoResponseDTO replaceThumbnail(UUID videoId, MultipartFile thumbnail, User currentUser) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (!video.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only edit your own videos");
        }

        if (thumbnail.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload empty thumbnail");
        }

        if (thumbnail.getSize() > 2 * 1024 * 1024) { //2mb max file size
            throw new IllegalArgumentException("Thumbnail file size exceeds 2MB limit");
        }

        String thumbContentType = thumbnail.getContentType();
        if (thumbContentType == null || (!thumbContentType.equals("image/jpeg") && !thumbContentType.equals("image/png") && !thumbContentType.equals("image/webp"))) {
            throw new IllegalArgumentException("Invalid thumbnail type. Only jpg, png, and webp are allowed");
        }

        String thumbOriginalFilename = thumbnail.getOriginalFilename();
        String thumbExtension = "";
        if (thumbOriginalFilename != null && thumbOriginalFilename.contains(".")) {
            thumbExtension = thumbOriginalFilename.substring(thumbOriginalFilename.lastIndexOf("."));
        }
        String thumbNewFilename = UUID.randomUUID().toString() + thumbExtension;
        String thumbRelativePath = "/uploads/thumbnails/" + thumbNewFilename;
        Path thumbTargetLocation = this.thumbnailStorageLocation.resolve(thumbNewFilename);

        try {
            Files.copy(thumbnail.getInputStream(), thumbTargetLocation);

            //delete replaced thumbnail file from disk
            String oldThumbnailUrl = video.getThumbnailUrl();
            if (oldThumbnailUrl != null && oldThumbnailUrl.startsWith("/uploads/thumbnails/")) {
                String oldFilename = oldThumbnailUrl.substring("/uploads/thumbnails/".length());
                if (!oldFilename.equals("placeholder.png")) {
                    Files.deleteIfExists(this.thumbnailStorageLocation.resolve(oldFilename));
                }
            }

            video.setThumbnailUrl(thumbRelativePath);
            Video savedVideo = videoRepository.save(video);
            return mapToDTO(savedVideo);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store thumbnail file. Please try again!", ex);
        }
    }

    //dynamically resolves video file path across different environments/machines
    public Path resolveVideoPath(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty()) {
            return null;
        }
        Path path = Paths.get(rawPath);
        if (path.isAbsolute() && Files.exists(path)) {
            return path;
        }
        Path fileName = path.getFileName();
        if (fileName != null) {
            Path target = this.fileStorageLocation.resolve(fileName.toString()).normalize();
            if (Files.exists(target)) {
                return target;
            }
            return target;
        }
        return this.fileStorageLocation.resolve(rawPath).normalize();
    }

    //video streaming file retrieval logic
    public Resource loadVideoAsResource(UUID videoId) {
        return loadVideoAsResource(videoId, null);
    }

    public Resource loadVideoAsResource(UUID videoId, User currentUser) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));
                
        if (!canUserAccessVideo(video, currentUser)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        Path filePath = resolveVideoPath(video.getFilePath());
        Resource resource = (filePath != null) ? new FileSystemResource(filePath) : null;
        
        if (resource != null && resource.exists()) {
            return resource;
        } else {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }
    }

    public org.springframework.data.domain.Page<VideoResponseDTO> getAllVideos(UUID uploaderId, String search, UUID categoryId, org.springframework.data.domain.Pageable pageable) {
        return getAllVideos(uploaderId, search, categoryId, pageable, null, false);
    }

    public org.springframework.data.domain.Page<VideoResponseDTO> getAllVideos(UUID uploaderId, String search, UUID categoryId, org.springframework.data.domain.Pageable pageable, User currentUser) {
        return getAllVideos(uploaderId, search, categoryId, pageable, currentUser, false);
    }

    //video database filtering and pagination logic
    public org.springframework.data.domain.Page<VideoResponseDTO> getAllVideos(UUID uploaderId, String search, UUID categoryId, org.springframework.data.domain.Pageable pageable, User currentUser, boolean includeHidden) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Category not found");
        }

        boolean isAdminOrMod = currentUser != null && ("ADMINISTRATOR".equals(currentUser.getRole()) || "CONTENT_MODERATOR".equals(currentUser.getRole()));
        boolean isSelf = currentUser != null && uploaderId != null && currentUser.getUserId().equals(uploaderId);

        org.springframework.data.domain.Page<Video> videoPage;
        boolean hasSearch = search != null && !search.trim().isEmpty();
        
        if (isSelf) {
            if (categoryId != null) {
                if (hasSearch) {
                    videoPage = videoRepository.findByOwner_UserIdAndCategory_IdAndTitleContainingIgnoreCase(uploaderId, categoryId, search, pageable);
                } else {
                    videoPage = videoRepository.findByOwner_UserIdAndCategory_Id(uploaderId, categoryId, pageable);
                }
            } else {
                if (hasSearch) {
                    videoPage = videoRepository.findByOwner_UserIdAndTitleContainingIgnoreCase(uploaderId, search, pageable);
                } else {
                    videoPage = videoRepository.findByOwner_UserId(uploaderId, pageable);
                }
            }
        } else if (isAdminOrMod && includeHidden) {
            if (uploaderId != null) {
                if (categoryId != null) {
                    if (hasSearch) {
                        videoPage = videoRepository.findByOwner_UserIdAndCategory_IdAndTitleContainingIgnoreCase(uploaderId, categoryId, search, pageable);
                    } else {
                        videoPage = videoRepository.findByOwner_UserIdAndCategory_Id(uploaderId, categoryId, pageable);
                    }
                } else {
                    if (hasSearch) {
                        videoPage = videoRepository.findByOwner_UserIdAndTitleContainingIgnoreCase(uploaderId, search, pageable);
                    } else {
                        videoPage = videoRepository.findByOwner_UserId(uploaderId, pageable);
                    }
                }
            } else {
                if (categoryId != null) {
                    if (hasSearch) {
                        videoPage = videoRepository.findByCategory_IdAndTitleContainingIgnoreCase(categoryId, search, pageable);
                    } else {
                        videoPage = videoRepository.findByCategory_Id(categoryId, pageable);
                    }
                } else {
                    if (hasSearch) {
                        videoPage = videoRepository.findByTitleContainingIgnoreCase(search, pageable);
                    } else {
                        videoPage = videoRepository.findAll(pageable);
                    }
                }
            }
        } else {
            if (uploaderId != null) {
                if (categoryId != null) {
                    if (hasSearch) {
                        videoPage = videoRepository.findPublicVideosByOwnerAndCategoryAndTitle(uploaderId, categoryId, search, pageable);
                    } else {
                        videoPage = videoRepository.findPublicVideosByOwnerAndCategory(uploaderId, categoryId, pageable);
                    }
                } else {
                    if (hasSearch) {
                        videoPage = videoRepository.findPublicVideosByOwnerAndTitle(uploaderId, search, pageable);
                    } else {
                        videoPage = videoRepository.findPublicVideosByOwner(uploaderId, pageable);
                    }
                }
            } else {
                if (categoryId != null) {
                    if (hasSearch) {
                        videoPage = videoRepository.findPublicVideosByCategoryAndTitle(categoryId, search, pageable);
                    } else {
                        videoPage = videoRepository.findPublicVideosByCategory(categoryId, pageable);
                    }
                } else {
                    if (hasSearch) {
                        videoPage = videoRepository.findPublicVideosByTitle(search, pageable);
                    } else {
                        videoPage = videoRepository.findPublicVideos(pageable);
                    }
                }
            }
        }
        
        return videoPage.map(this::mapToDTO);
    }

    public VideoResponseDTO getVideoDetails(UUID videoId) {
        return getVideoDetails(videoId, null);
    }

    public VideoResponseDTO getVideoDetails(UUID videoId, User currentUser) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));
        
        if (!canUserAccessVideo(video, currentUser)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        return mapToDTO(video);
    }

    @Transactional
    public VideoResponseDTO toggleVideoHidden(UUID videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));
        boolean newHidden = !video.isHidden();
        video.setHidden(newHidden);
        if (newHidden) {
            video.setPrivacy("PRIVATE");
        } else {
            video.setPrivacy("PUBLIC");
        }
        Video saved = videoRepository.save(video);
        return mapToDTO(saved);
    }

    public VideoResponseDTO updateVideo(UUID videoId, String title, String description, UUID categoryId, User currentUser) {
        return updateVideo(videoId, title, description, categoryId, null, currentUser);
    }

    public VideoResponseDTO updateVideo(UUID videoId, String title, String description, UUID categoryId, String privacy, User currentUser) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        if (!video.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only edit your own videos");
        }

        if (title != null && !title.trim().isEmpty()) {
            video.setTitle(title.trim());
        }
        if (description != null) {
            video.setDescription(description.trim());
        }
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Category not found"));
            video.setCategory(category);
        }
        if (privacy != null && !privacy.trim().isEmpty()) {
            String p = privacy.trim().toUpperCase();
            if ("PUBLIC".equals(p) || "PRIVATE".equals(p)) {
                video.setPrivacy(p);
            }
        }

        Video savedVideo = videoRepository.save(video);
        return mapToDTO(savedVideo);
    }

    @Transactional
    public void deleteVideo(UUID videoId, User currentUser) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
                
        if (!video.getOwner().getUserId().equals(currentUser.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only delete your own videos");
        }
        
        deleteVideoInternal(video, null, null);
    }

    @Transactional
    //moderation video removal logic
    public void moderateDeleteVideo(UUID videoId) {
        moderateDeleteVideo(videoId, "Violation of platform policies", null);
    }

    @Transactional
    public void moderateDeleteVideo(UUID videoId, String reason, User moderator) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
                
        deleteVideoInternal(video, reason, moderator);
    }

    @Transactional
    public void deleteVideoInternal(Video video, String reason, User moderator) {
        if (video == null) return;
        UUID videoId = video.getVideoId();
        User owner = video.getOwner();
        String videoTitle = video.getTitle();

        // 1. Delete physical files from disk
        try {
            Path videoPath = resolveVideoPath(video.getFilePath());
            if (videoPath != null) {
                Files.deleteIfExists(videoPath);
            }
            String thumbUrl = video.getThumbnailUrl();
            if (thumbUrl != null && thumbUrl.startsWith("/uploads/thumbnails/")) {
                String thumbFilename = thumbUrl.substring("/uploads/thumbnails/".length());
                if (!thumbFilename.equals("placeholder.png")) {
                    Files.deleteIfExists(this.thumbnailStorageLocation.resolve(thumbFilename));
                }
            }
        } catch (IOException e) {
            System.err.println("Could not delete physical file: " + e.getMessage());
        }
        
        // 2. Clean all dependent relations in proper database order
        try {
            entityManager.createNativeQuery("DELETE FROM VideoViewEvent WHERE video_id = :id")
                .setParameter("id", videoId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM watch_history WHERE video_id = :id")
                .setParameter("id", videoId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM playlist_video WHERE video_id = :id")
                .setParameter("id", videoId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM [Comment] WHERE video_id = :id AND parent_comment_id IS NOT NULL")
                .setParameter("id", videoId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM [Comment] WHERE video_id = :id")
                .setParameter("id", videoId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM [Like] WHERE video_id = :id")
                .setParameter("id", videoId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM [dislike] WHERE video_id = :id")
                .setParameter("id", videoId).executeUpdate();
            entityManager.createNativeQuery("DELETE FROM [notification] WHERE related_entity_type = 'VIDEO' AND related_entity_id = :id")
                .setParameter("id", videoId).executeUpdate();
            entityManager.flush();
        } catch (Exception e) {
            System.err.println("Error cleaning dependent video relations: " + e.getMessage());
        }

        // 3. Remove Video record entirely from database
        try {
            videoRepository.delete(video);
            entityManager.flush();
        } catch (Exception e) {
            System.err.println("Fallback delete for video entity: " + e.getMessage());
            try {
                entityManager.createNativeQuery("DELETE FROM Video WHERE video_id = :id")
                    .setParameter("id", videoId).executeUpdate();
                entityManager.flush();
            } catch (Exception ex2) {
                System.err.println("Direct native delete also failed: " + ex2.getMessage());
            }
        }

        // 4. Send notification to video owner
        if (owner != null && notificationService != null) {
            try {
                if (moderator != null || (reason != null && !reason.trim().isEmpty())) {
                    String cleanReason = (reason != null && !reason.trim().isEmpty()) ? reason.trim() : "Violation of platform community guidelines";
                    notificationService.createNotification(
                        owner.getUserId(),
                        "MODERATION_ACTION",
                        "Video Removed by Moderation",
                        "Your video \"" + videoTitle + "\" was removed by a moderator. Reason: " + cleanReason + ".",
                        "VIDEO",
                        videoId
                    );
                } else {
                    notificationService.createNotification(
                        owner.getUserId(),
                        "VIDEO_DELETE",
                        "Video Deleted",
                        "Your video \"" + videoTitle + "\" has been successfully deleted.",
                        "VIDEO",
                        videoId
                    );
                }
            } catch (Exception notifEx) {
                System.err.println("Failed to send video deletion notification: " + notifEx.getMessage());
            }
        }
    }

    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    @Transactional
    public void cleanupOrphanedAndModeratedVideos() {
        try {
            // Fix previous actions: Delete video records that were marked as DELETED in resolved reports
            List<UUID> reportedDeletedVideoIds = entityManager.createQuery(
                "SELECT DISTINCT r.reportableId FROM Report r WHERE r.reportableType = 'VIDEO' AND r.status = 'RESOLVED' AND r.moderatorAction = 'DELETED' AND r.reportableId IS NOT NULL", 
                UUID.class
            ).getResultList();

            if (reportedDeletedVideoIds != null) {
                for (UUID vid : reportedDeletedVideoIds) {
                    if (vid != null) {
                        videoRepository.findById(vid).ifPresent(v -> {
                            System.out.println("Cleaning up previously moderated/deleted video: " + vid + " (" + v.getTitle() + ")");
                            deleteVideoInternal(v, "Moderation resolved deletion", null);
                        });
                    }
                }
            }

            // Also cleanup videos whose physical video file no longer exists on disk
            List<Video> allVideos = videoRepository.findAll();
            for (Video v : allVideos) {
                if (v.getFilePath() != null && !v.getFilePath().trim().isEmpty()) {
                    Path p = resolveVideoPath(v.getFilePath());
                    if (p == null || !Files.exists(p)) {
                        System.out.println("Cleaning up ghost video with missing physical file: " + v.getVideoId() + " (" + v.getTitle() + ")");
                        deleteVideoInternal(v, "Missing physical file cleanup", null);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error running video cleanup on startup: " + e.getMessage());
        }
    }

    @Transactional
    public void incrementViewCount(UUID videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new com.streamhub.common.exception.ResourceNotFoundException("Video not found with id " + videoId));

        // TODO(Jayalath): anti-spam validation still applies here (existing stub, unchanged) — once he caps repeat views, that cap should logically also prevent spamming VideoViewEvent rows, not just view_count.
        //increment view counter without deduplication
        video.setViewCount(video.getViewCount() + 1);
        videoRepository.save(video);

        //record rolling watch event
        if (videoViewEventRepository != null) {
            com.streamhub.watchhistory.entity.VideoViewEvent event = new com.streamhub.watchhistory.entity.VideoViewEvent(video, user, java.time.LocalDateTime.now());
            videoViewEventRepository.save(event);
        }
    }

    @Transactional
    public void incrementViewCount(UUID videoId) {
        User currentUser = null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String username = auth.getName();
                currentUser = entityManager.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                        .setParameter("username", username)
                        .getResultStream()
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception ignored) {}
        incrementViewCount(videoId, currentUser);
    }
    
    private VideoResponseDTO mapToDTO(Video video) {
        CategoryDto categoryDto = null;
        if (video.getCategory() != null) {
            categoryDto = new CategoryDto(
                video.getCategory().getId(),
                video.getCategory().getName(),
                video.getCategory().getDescription()
            );
        }

        VideoResponseDTO dto = new VideoResponseDTO(
                video.getVideoId(),
                video.getTitle(),
                video.getDescription(),
                video.getOwner().getUserId(),
                video.getOwner().getUsername(),
                video.getOwner().getAvatarUrl(),
                video.getThumbnailUrl(),
                video.getCreatedAt(),
                video.getViewCount(),
                categoryDto,
                video.getOwner().getUserId(),
                video.getOwner().getFirstName() + " " + video.getOwner().getLastName(),
                video.getOwner().getAvatarUrl()
        );
        dto.setHidden(video.isHidden());
        dto.setPrivacy(video.getPrivacy());
        dto.setDurationSeconds(video.getDurationSeconds());
        if (video.getDurationSeconds() != null && video.getDurationSeconds() > 0) {
            int sec = video.getDurationSeconds();
            int mins = sec / 60;
            int remSec = sec % 60;
            int hours = mins / 60;
            if (hours > 0) {
                dto.setDuration(String.format("%d:%02d:%02d", hours, mins % 60, remSec));
            } else {
                dto.setDuration(String.format("%d:%02d", mins, remSec));
            }
        }
        return dto;
    }
}
