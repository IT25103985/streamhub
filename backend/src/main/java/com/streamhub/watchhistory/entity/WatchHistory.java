package com.streamhub.watchhistory.entity;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "watch_history")
//database entity tracking user video watch progress and timestamp
public class WatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "watch_history_id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "last_position", nullable = false)
    private Integer lastPosition = 0;

    @Column(name = "watch_duration_seconds")
    private Integer watchDurationSeconds;

    @CreationTimestamp
    @Column(name = "watched_at", nullable = false, updatable = false)
    private LocalDateTime watchedAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WatchHistory() {}

    public WatchHistory(User user, Video video) {
        this.user = user;
        this.video = video;
        this.lastPosition = 0;
    }

    public WatchHistory(User user, Video video, Integer lastPosition) {
        this.user = user;
        this.video = video;
        this.lastPosition = lastPosition != null ? lastPosition : 0;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
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
