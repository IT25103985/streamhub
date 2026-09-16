package com.streamhub.social.dto;

//response object returning like count dislike count and user state
public class LikeStatusDTO {
    private int likeCount;
    private boolean userLiked;
    private int dislikeCount;
    private boolean userDisliked;

    public LikeStatusDTO() {}

    public LikeStatusDTO(int likeCount, boolean userLiked, int dislikeCount, boolean userDisliked) {
        this.likeCount = likeCount;
        this.userLiked = userLiked;
        this.dislikeCount = dislikeCount;
        this.userDisliked = userDisliked;
    }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public boolean isUserLiked() { return userLiked; }
    public void setUserLiked(boolean userLiked) { this.userLiked = userLiked; }

    public int getDislikeCount() { return dislikeCount; }
    public void setDislikeCount(int dislikeCount) { this.dislikeCount = dislikeCount; }

    public boolean isUserDisliked() { return userDisliked; }
    public void setUserDisliked(boolean userDisliked) { this.userDisliked = userDisliked; }
}
