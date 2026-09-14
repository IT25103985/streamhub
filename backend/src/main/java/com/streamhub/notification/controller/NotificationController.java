package com.streamhub.notification.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.notification.dto.NotificationResponse;
import com.streamhub.notification.dto.UnreadCountResponse;
import com.streamhub.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@PreAuthorize("isAuthenticated()")
//endpoints for fetching and managing user notifications
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    public NotificationController(NotificationService notificationService, UserRepository userRepository) {
        this.notificationService = notificationService;
        this.userRepository = userRepository;
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(notificationService.getUserNotifications(user.getUserId(), PageRequest.of(page, size)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Principal principal) {
        User user = getCurrentUser(principal);
        long count = notificationService.getUnreadCount(user.getUserId());
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @PatchMapping("/{id}/read")
    //marks a single notification as read
    public ResponseEntity<Void> markAsRead(
            Principal principal,
            @PathVariable UUID id) {
        User user = getCurrentUser(principal);
        notificationService.markAsRead(id, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(Principal principal) {
        User user = getCurrentUser(principal);
        notificationService.markAllAsRead(user.getUserId());
        return ResponseEntity.noContent().build();
    }
}
