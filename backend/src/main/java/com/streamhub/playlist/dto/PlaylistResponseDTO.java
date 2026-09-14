package com.streamhub.playlist.dto;

import java.time.LocalDateTime;
import java.util.UUID;

//summary payload representing a playlist card preview
public class PlaylistResponseDTO {

    private UUID playlistId;
    private String name;
    private String ownerUsername;
    private String privacy;
    private String thumbnailUrl;
    private boolean isHidden;
    private int videoCount;
    private LocalDateTime createdAt;

    public PlaylistResponseDTO() {}

    public PlaylistResponseDTO(UUID playlistId, String name, String ownerUsername, String privacy, String thumbnailUrl, boolean isHidden, int videoCount, LocalDateTime createdAt) {
        this.playlistId = playlistId;
        this.name = name;
        this.ownerUsername = ownerUsername;
        this.privacy = privacy;
        this.thumbnailUrl = thumbnailUrl;
        this.isHidden = isHidden;
        this.videoCount = videoCount;
        this.createdAt = createdAt;
    }

    public UUID getPlaylistId() { return playlistId; }
    public void setPlaylistId(UUID playlistId) { this.playlistId = playlistId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public String getPrivacy() { return privacy; }
    public void setPrivacy(String privacy) { this.privacy = privacy; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { isHidden = hidden; }

    public int getVideoCount() { return videoCount; }
    public void setVideoCount(int videoCount) { this.videoCount = videoCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
