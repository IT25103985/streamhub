package com.streamhub.watchhistory.dto;

import com.streamhub.video.dto.VideoResponseDTO;

import java.time.LocalDateTime;
import java.util.UUID;

//response object returning watched video item and last position
public class WatchHistoryResponseDTO {

    private UUID watchHistoryId;
    private VideoResponseDTO video;
    private Integer lastPosition;
    private Integer watchDurationSeconds;
    private LocalDateTime watchedAt;
    private LocalDateTime updatedAt;

    public WatchHistoryResponseDTO() {}

    public WatchHistoryResponseDTO(UUID watchHistoryId, VideoResponseDTO video, LocalDateTime watchedAt) {
        this.watchHistoryId = watchHistoryId;
        this.video = video;
        this.watchedAt = watchedAt;
        this.lastPosition = 0;
    }

    public WatchHistoryResponseDTO(UUID watchHistoryId, VideoResponseDTO video, LocalDateTime watchedAt, Integer watchDurationSeconds) {
        this.watchHistoryId = watchHistoryId;
        this.video = video;
        this.watchedAt = watchedAt;
        this.watchDurationSeconds = watchDurationSeconds;
        this.lastPosition = watchDurationSeconds != null ? watchDurationSeconds : 0;
    }

    public WatchHistoryResponseDTO(UUID watchHistoryId, VideoResponseDTO video, Integer lastPosition, LocalDateTime watchedAt, LocalDateTime updatedAt) {
        this.watchHistoryId = watchHistoryId;
        this.video = video;
        this.lastPosition = lastPosition != null ? lastPosition : 0;
        this.watchDurationSeconds = this.lastPosition;
        this.watchedAt = watchedAt;
        this.updatedAt = updatedAt;
    }

    public UUID getWatchHistoryId() {
        return watchHistoryId;
    }

    public void setWatchHistoryId(UUID watchHistoryId) {
        this.watchHistoryId = watchHistoryId;
    }

    public VideoResponseDTO getVideo() {
        return video;
    }

    public void setVideo(VideoResponseDTO video) {
        this.video = video;
    }

    public Integer getLastPosition() {
        return lastPosition;
    }

    public void setLastPosition(Integer lastPosition) {
        this.lastPosition = lastPosition != null ? lastPosition : 0;
    }

    public Integer getWatchDurationSeconds() {
        return watchDurationSeconds;
    }

    public void setWatchDurationSeconds(Integer watchDurationSeconds) {
        this.watchDurationSeconds = watchDurationSeconds;
    }

    public LocalDateTime getWatchedAt() {
        return watchedAt;
    }

    public void setWatchedAt(LocalDateTime watchedAt) {
        this.watchedAt = watchedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
