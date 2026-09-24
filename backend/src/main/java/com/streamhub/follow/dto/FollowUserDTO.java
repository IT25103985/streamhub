package com.streamhub.follow.dto;

import java.util.UUID;

//data transfer object representing a user in follower and following lists
public class FollowUserDTO {
    private UUID userId;
    private String username;
    private String firstName;
    private String lastName;
    private String displayName;
    private String avatarUrl;

    public FollowUserDTO() {
    }

    public FollowUserDTO(UUID userId, String username, String firstName, String lastName, String avatarUrl) {
        this.userId = userId;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        String full = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
        this.displayName = !full.isEmpty() ? full : username;
        this.avatarUrl = avatarUrl;
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

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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
}
