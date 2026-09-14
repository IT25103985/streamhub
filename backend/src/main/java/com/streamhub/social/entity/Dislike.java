package com.streamhub.social.entity;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "dislike", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"video_id", "user_id"})
})
//database entity tracking user dislike reaction on a video
public class Dislike {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "dislike_id", updatable = false, nullable = false)
    private UUID dislikeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Dislike() {}

    public Dislike(Video video, User user) {
        this.video = video;
        this.user = user;
    }

    public UUID getDislikeId() { return dislikeId; }
    public void setDislikeId(UUID dislikeId) { this.dislikeId = dislikeId; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
