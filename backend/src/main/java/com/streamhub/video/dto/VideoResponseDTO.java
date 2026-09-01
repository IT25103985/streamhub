package com.streamhub.video.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import com.streamhub.category.dto.CategoryDto;

//response object representing video metadata views and uploader info
public class VideoResponseDTO {
    private UUID videoId;
    private String title;
    private String description;
    private UUID ownerId;
    private String ownerUsername;
    private String ownerAvatarUrl;
    private String thumbnailUrl;
    private LocalDateTime createdAt;
    private CategoryDto category;

    private int viewCount;

    private UUID uploaderId;
    private String uploaderName;
    private String uploaderAvatarUrl;

    private boolean isHidden;
    private String privacy = "PUBLIC";
    private Integer durationSeconds;
    private String duration;

    public VideoResponseDTO() {}

    public VideoResponseDTO(UUID videoId, String title, String description, UUID ownerId, String ownerUsername, String ownerAvatarUrl, String thumbnailUrl, LocalDateTime createdAt, CategoryDto category) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.ownerAvatarUrl = ownerAvatarUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = createdAt;
        this.category = category;
        this.viewCount = 0;
    }

    public VideoResponseDTO(UUID videoId, String title, String description, UUID ownerId, String ownerUsername, String ownerAvatarUrl, String thumbnailUrl, LocalDateTime createdAt, CategoryDto category, UUID uploaderId, String uploaderName, String uploaderAvatarUrl) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.ownerAvatarUrl = ownerAvatarUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = createdAt;
        this.category = category;
        this.viewCount = 0;
        this.uploaderId = uploaderId;
        this.uploaderName = uploaderName;
        this.uploaderAvatarUrl = uploaderAvatarUrl;
    }

    public VideoResponseDTO(UUID videoId, String title, String description, UUID ownerId, String ownerUsername, String ownerAvatarUrl, String thumbnailUrl, LocalDateTime createdAt, int viewCount, CategoryDto category) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.ownerAvatarUrl = ownerAvatarUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = createdAt;
        this.viewCount = viewCount;
        this.category = category;
    }

    public VideoResponseDTO(UUID videoId, String title, String description, UUID ownerId, String ownerUsername, String ownerAvatarUrl, String thumbnailUrl, LocalDateTime createdAt, int viewCount, CategoryDto category, UUID uploaderId, String uploaderName, String uploaderAvatarUrl) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.ownerAvatarUrl = ownerAvatarUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.createdAt = createdAt;
        this.viewCount = viewCount;
        this.category = category;
        this.uploaderId = uploaderId;
        this.uploaderName = uploaderName;
        this.uploaderAvatarUrl = uploaderAvatarUrl;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerAvatarUrl() {
        return ownerAvatarUrl;
    }

    public void setOwnerAvatarUrl(String ownerAvatarUrl) {
        this.ownerAvatarUrl = ownerAvatarUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public CategoryDto getCategory() {
        return category;
    }

    public void setCategory(CategoryDto category) {
        this.category = category;
    }

    public UUID getUploaderId() {
        return uploaderId;
    }

    public void setUploaderId(UUID uploaderId) {
        this.uploaderId = uploaderId;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getUploaderAvatarUrl() {
        return uploaderAvatarUrl;
    }

    public void setUploaderAvatarUrl(String uploaderAvatarUrl) {
        this.uploaderAvatarUrl = uploaderAvatarUrl;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = privacy;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }
}
