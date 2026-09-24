package com.streamhub.follow.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

//data transfer object representing follower and following counts and relationship status
public class FollowCountsResponse {

    private UUID userId;
    private long followerCount;
    private long followingCount;

    @JsonProperty("isFollowing")
    private Boolean isFollowing;

    public FollowCountsResponse() {
    }

    public FollowCountsResponse(UUID userId, long followerCount, long followingCount, Boolean isFollowing) {
        this.userId = userId;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.isFollowing = isFollowing;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public long getFollowerCount() {
        return followerCount;
    }

    public void setFollowerCount(long followerCount) {
        this.followerCount = followerCount;
    }

    public long getFollowingCount() {
        return followingCount;
    }

    public void setFollowingCount(long followingCount) {
        this.followingCount = followingCount;
    }

    public Boolean getIsFollowing() {
        return isFollowing;
    }

    public void setIsFollowing(Boolean following) {
        isFollowing = following;
    }
}
