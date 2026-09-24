package com.streamhub.follow.dto;

import java.util.UUID;

public class SidebarFollowedUserDTO {
    private UUID userId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private long viewsLast7Days;

    public SidebarFollowedUserDTO() {}

    public SidebarFollowedUserDTO(UUID userId, String username, String firstName, String lastName, String avatarUrl, long viewsLast7Days) {
        this.userId = userId;
        this.username = username;
        String full = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        this.displayName = !full.isEmpty() ? full : username;
        this.avatarUrl = avatarUrl;
        this.viewsLast7Days = viewsLast7Days;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public long getViewsLast7Days() {
        return viewsLast7Days;
    }

    public void setViewsLast7Days(long viewsLast7Days) {
        this.viewsLast7Days = viewsLast7Days;
    }
}
