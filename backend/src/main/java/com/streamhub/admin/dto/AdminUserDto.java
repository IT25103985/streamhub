package com.streamhub.admin.dto;

import com.streamhub.common.entity.User;
import java.time.LocalDateTime;
import java.util.UUID;

//data transfer object representing user account details in admin panel
public class AdminUserDto {
    private UUID userId;
    private String username;
    private String email;
    private String role;
    private String status;
    private LocalDateTime joinedDate;

    public AdminUserDto(User user) {
        this.userId = user.getUserId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.joinedDate = user.getCreatedAt();
    }

    public UUID getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public String getStatus() { return status; }
    public LocalDateTime getJoinedDate() { return joinedDate; }
}
