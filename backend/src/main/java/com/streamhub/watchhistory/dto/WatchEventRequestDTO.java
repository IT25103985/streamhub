package com.streamhub.watchhistory.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

//payload recording playback timestamp progress for a video
public class WatchEventRequestDTO {

    @NotNull(message = "Video ID is required")
    private UUID videoId;

    private Integer positionSeconds;

    //optional row id from first call, skips latestRow lookup on heartbeats
    private UUID watchHistoryId;

    public WatchEventRequestDTO() {}

    public WatchEventRequestDTO(UUID videoId, Integer positionSeconds) {
        this.videoId = videoId;
        this.positionSeconds = positionSeconds;
    }

    public UUID getVideoId() {
        return videoId;
    }

    public void setVideoId(UUID videoId) {
        this.videoId = videoId;
    }

    public Integer getPositionSeconds() {
        return positionSeconds;
    }

    public void setPositionSeconds(Integer positionSeconds) {
        this.positionSeconds = positionSeconds;
    }

    public UUID getWatchHistoryId() {
        return watchHistoryId;
    }

    public void setWatchHistoryId(UUID watchHistoryId) {
        this.watchHistoryId = watchHistoryId;
    }
}
