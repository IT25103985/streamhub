package com.streamhub.video.entity;

import com.streamhub.common.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Video")
//database entity storing uploaded video file path thumbnail and view count
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "video_id", updatable = false, nullable = false)
    private UUID videoId;

    @org.hibernate.annotations.Nationalized
    @Column(name = "title", nullable = false, columnDefinition = "NVARCHAR(255)")
    private String title;

    @org.hibernate.annotations.Nationalized
    @Column(name = "description", columnDefinition = "NVARCHAR(MAX)")
    private String description;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "thumbnail_url", nullable = false, length = 255)
    private String thumbnailUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private com.streamhub.category.entity.Category category;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "is_hidden", nullable = false)
    private boolean isHidden = false;

    @Column(name = "privacy", nullable = false, length = 10)
    private String privacy = "PUBLIC";

    @OneToMany(mappedBy = "video", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<com.streamhub.social.entity.Like> likes = new ArrayList<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<com.streamhub.social.entity.Dislike> dislikes = new ArrayList<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<com.streamhub.social.entity.Comment> comments = new ArrayList<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<com.streamhub.playlist.entity.PlaylistVideo> playlistVideos = new ArrayList<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<com.streamhub.watchhistory.entity.WatchHistory> watchHistories = new ArrayList<>();

    @OneToMany(mappedBy = "video", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<com.streamhub.watchhistory.entity.VideoViewEvent> videoViewEvents = new ArrayList<>();

    public Video() {}

    public Video(String title, String description, String filePath, String thumbnailUrl, User owner) {
        this.title = title;
        this.description = description;
        this.filePath = filePath;
        this.thumbnailUrl = thumbnailUrl;
        this.owner = owner;
        this.privacy = "PUBLIC";
    }

    public Video(String title, String description, String filePath, String thumbnailUrl, User owner, String privacy) {
        this.title = title;
        this.description = description;
        this.filePath = filePath;
        this.thumbnailUrl = thumbnailUrl;
        this.owner = owner;
        this.privacy = (privacy != null && !privacy.trim().isEmpty()) ? privacy.trim().toUpperCase() : "PUBLIC";
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public com.streamhub.category.entity.Category getCategory() {
        return category;
    }

    public void setCategory(com.streamhub.category.entity.Category category) {
        this.category = category;
    }

    public int getViewCount() {
        return viewCount;
    }

    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public void setHidden(boolean hidden) {
        isHidden = hidden;
    }

    public String getPrivacy() {
        return privacy;
    }

    public void setPrivacy(String privacy) {
        this.privacy = (privacy != null && !privacy.trim().isEmpty()) ? privacy.trim().toUpperCase() : "PUBLIC";
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}
