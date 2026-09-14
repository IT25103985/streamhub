package com.streamhub.watchhistory.entity;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "VideoViewEvent", indexes = {
    @Index(name = "IX_VideoViewEvent_Video_ViewedAt", columnList = "video_id, viewed_at")
})
//database entity logging individual video playback start events with timestamp
public class VideoViewEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    public VideoViewEvent() {}

    public VideoViewEvent(Video video, User user, LocalDateTime viewedAt) {
        this.video = video;
        this.user = user;
        this.viewedAt = viewedAt != null ? viewedAt : LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (viewedAt == null) {
            viewedAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Video getVideo() {
        return video;
    }

    public void setVideo(Video video) {
        this.video = video;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getViewedAt() {
        return viewedAt;
    }

    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
}
