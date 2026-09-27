package com.streamhub.follow.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.follow.dto.FollowCountsResponse;
import com.streamhub.follow.dto.FollowUserDTO;
import com.streamhub.follow.service.FollowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/follows")
//rest controller exposing endpoints for following unfollowing and querying follow counts and lists
public class FollowController {

    private final FollowService followService;
    private final UserRepository userRepository;

    public FollowController(FollowService followService, UserRepository userRepository) {
        this.followService = followService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(authentication.getName())
                .orElse(null));
    }

    //follow target user endpoint
    @PostMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> follow(@PathVariable UUID userId, Authentication authentication) {
        User requester = getAuthenticatedUser(authentication);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        followService.follow(requester.getUserId(), userId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    //unfollow target user endpoint
    @DeleteMapping("/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> unfollow(@PathVariable UUID userId, Authentication authentication) {
        User requester = getAuthenticatedUser(authentication);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        followService.unfollow(requester.getUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    //get follower and following counts + isFollowing status
    @GetMapping("/{userId}/counts")
    public ResponseEntity<FollowCountsResponse> getCounts(@PathVariable UUID userId, Authentication authentication) {
        User requester = getAuthenticatedUser(authentication);
        UUID requesterId = requester != null ? requester.getUserId() : null;
        return ResponseEntity.ok(followService.getCounts(userId, requesterId));
    }

    //get list of followers (owner only)
    @GetMapping("/{userId}/followers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<FollowUserDTO>> getFollowers(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User requester = getAuthenticatedUser(authentication);
        UUID requesterId = requester != null ? requester.getUserId() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(followService.getFollowers(userId, requesterId, pageable));
    }

    //get list of following users (owner only)
    @GetMapping("/{userId}/following")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<FollowUserDTO>> getFollowing(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {
        User requester = getAuthenticatedUser(authentication);
        UUID requesterId = requester != null ? requester.getUserId() : null;
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return ResponseEntity.ok(followService.getFollowing(userId, requesterId, pageable));
    }

    //retrieves followed creators with latest videos for left sidebar display
    @GetMapping("/me/sidebar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<com.streamhub.follow.dto.SidebarFollowResponse> getSidebarFollowing(Authentication authentication) {
        User requester = getAuthenticatedUser(authentication);
        if (requester == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return ResponseEntity.ok(followService.getSidebarFollowing(requester.getUserId()));
    }
}
