package com.streamhub.social.dto;

import java.time.LocalDateTime;
import java.util.UUID;

//data transfer object for comment text author info and nested replies
public class CommentDTO {

    private UUID commentId;
    private UUID videoId;
    private String videoTitle;
    private UUID authorId;
    private String authorUsername;
    private String authorName;
    private String authorAvatarUrl;
    private String text;
    private LocalDateTime createdAt;
    private UUID parentCommentId;
    private java.util.List<CommentDTO> replies;

    public CommentDTO() {}

    public CommentDTO(UUID commentId, UUID videoId, String videoTitle, UUID authorId, String authorUsername, String authorAvatarUrl, String text, LocalDateTime createdAt, UUID parentCommentId, java.util.List<CommentDTO> replies) {
        this.commentId = commentId;
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.authorAvatarUrl = authorAvatarUrl;
        this.text = text;
        this.createdAt = createdAt;
        this.parentCommentId = parentCommentId;
        this.replies = replies;
    }

    public CommentDTO(UUID commentId, UUID videoId, String videoTitle, UUID authorId, String authorUsername, String authorName, String authorAvatarUrl, String text, LocalDateTime createdAt, UUID parentCommentId, java.util.List<CommentDTO> replies) {
        this.commentId = commentId;
        this.videoId = videoId;
        this.videoTitle = videoTitle;
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.authorName = authorName;
        this.authorAvatarUrl = authorAvatarUrl;
        this.text = text;
        this.createdAt = createdAt;
        this.parentCommentId = parentCommentId;
        this.replies = replies;
    }

    public UUID getCommentId() { return commentId; }
    public void setCommentId(UUID commentId) { this.commentId = commentId; }

    public UUID getVideoId() { return videoId; }
    public void setVideoId(UUID videoId) { this.videoId = videoId; }

    public String getVideoTitle() { return videoTitle; }
    public void setVideoTitle(String videoTitle) { this.videoTitle = videoTitle; }

    public UUID getAuthorId() { return authorId; }
    public void setAuthorId(UUID authorId) { this.authorId = authorId; }

    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getAuthorAvatarUrl() { return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) { this.authorAvatarUrl = authorAvatarUrl; }

    public UUID getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(UUID parentCommentId) { this.parentCommentId = parentCommentId; }

    public java.util.List<CommentDTO> getReplies() { return replies; }
    public void setReplies(java.util.List<CommentDTO> replies) { this.replies = replies; }
}
