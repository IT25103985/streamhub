package com.streamhub.playlist.dto;

import com.streamhub.video.dto.VideoResponseDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

//response object with complete playlist info and ordered video items
public class PlaylistDetailDTO {

    private UUID playlistId;
    private String name;
    private String ownerUsername;
    private String privacy;
    private String description;
    private String thumbnailUrl;
    private boolean isHidden;
    private List<VideoResponseDTO> videos;
    private LocalDateTime createdAt;

    public PlaylistDetailDTO() {}

    public PlaylistDetailDTO(UUID playlistId, String name, String ownerUsername, String privacy, String description, String thumbnailUrl, boolean isHidden, List<VideoResponseDTO> videos, LocalDateTime createdAt) {
        this.playlistId = playlistId;
        this.name = name;
        this.ownerUsername = ownerUsername;
        this.privacy = privacy;
        this.description = description;
        this.thumbnailUrl = thumbnailUrl;
        this.isHidden = isHidden;
        this.videos = videos;
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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

    public boolean isHidden() { return isHidden; }
    public void setHidden(boolean hidden) { this.isHidden = hidden; }

    public List<VideoResponseDTO> getVideos() { return videos; }
    public void setVideos(List<VideoResponseDTO> videos) { this.videos = videos; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
