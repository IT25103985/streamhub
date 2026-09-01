package com.streamhub.auth.dto;

import java.util.UUID;

//response payload with jwt token and user session details
public class AuthResponse {
    private String token;
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private String avatarUrl;
    private boolean needsOnboarding;

    public AuthResponse() {}

    public AuthResponse(String token, UUID userId, String username, String email, String role, String avatarUrl) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.needsOnboarding = false;
    }

    public AuthResponse(String token, UUID userId, String username, String email, String role, String avatarUrl, boolean needsOnboarding) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.needsOnboarding = needsOnboarding;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isNeedsOnboarding() { return needsOnboarding; }
    public void setNeedsOnboarding(boolean needsOnboarding) { this.needsOnboarding = needsOnboarding; }
}
