package com.streamhub.video.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class VideoResponseDTO {
    private UUID videoId;
    private String title;
    private String description;
    private UUID ownerId;
    private String ownerUsername;
    private LocalDateTime createdAt;

    public VideoResponseDTO() {}

    public VideoResponseDTO(UUID videoId, String title, String description, UUID ownerId, String ownerUsername, LocalDateTime createdAt) {
        this.videoId = videoId;
        this.title = title;
        this.description = description;
        this.ownerId = ownerId;
        this.ownerUsername = ownerUsername;
        this.createdAt = createdAt;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
