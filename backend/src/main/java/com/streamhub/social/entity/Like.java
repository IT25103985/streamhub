package com.streamhub.social.entity;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Like", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"video_id", "user_id"})
})
//database entity tracking user like reaction on a video
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "like_id", updatable = false, nullable = false)
    private UUID likeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false, foreignKey = @ForeignKey(name = "FK_Like_Video"))
    private Video video;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "FK_Like_User"))
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Like() {}

    public Like(Video video, User user) {
        this.video = video;
        this.user = user;
    }

    public UUID getLikeId() { return likeId; }
    public void setLikeId(UUID likeId) { this.likeId = likeId; }

    public Video getVideo() { return video; }
    public void setVideo(Video video) { this.video = video; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
