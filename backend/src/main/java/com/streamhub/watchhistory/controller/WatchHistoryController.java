package com.streamhub.watchhistory.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.watchhistory.dto.WatchEventRequestDTO;
import com.streamhub.watchhistory.dto.WatchHistoryResponseDTO;
import com.streamhub.watchhistory.service.WatchHistoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/v1/watch-history", "/api/watch-history"})
//controller for tracking playback progress and personalized recommendations
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;
    private final UserRepository userRepository;

    public WatchHistoryController(WatchHistoryService watchHistoryService, UserRepository userRepository) {
        this.watchHistoryService = watchHistoryService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }

    //watch history event recording endpoint
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    //records or updates playback timestamp for user history
    public ResponseEntity<Void> recordWatchEvent(
            Principal principal,
            @Valid @RequestBody WatchEventRequestDTO request) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        watchHistoryService.recordWatchHistory(user, request.getVideoId(), request.getPositionSeconds());
        return ResponseEntity.ok().build();
    }

    //records playback progress for video with optional duration
    @PostMapping("/record/{videoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> recordWatchHistory(
            Principal principal,
            @PathVariable UUID videoId,
            @RequestParam(required = false) Integer duration) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        watchHistoryService.recordWatchHistory(user, videoId, duration);
        return ResponseEntity.ok().build();
    }

    //retrieves paginated watch history for authenticated user
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<WatchHistoryResponseDTO>> getWatchHistory(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(watchHistoryService.getUserWatchHistory(user, page, size));
    }

    //clears entire watch history for current user
    @DeleteMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearWatchHistory(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        watchHistoryService.clearUserHistory(user);
        return ResponseEntity.noContent().build();
    }

    //removes specific video from user watch history
    @DeleteMapping("/{videoId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeVideoFromHistory(
            Principal principal,
            @PathVariable UUID videoId) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        watchHistoryService.deleteSingleHistory(user, videoId);
        return ResponseEntity.noContent().build();
    }

    //personalized recommendations endpoint
    @GetMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
    //generates recommended videos based on watched category affinity
    public ResponseEntity<List<VideoResponseDTO>> getRecommendations(
            Principal principal,
            @RequestParam(defaultValue = "10") int limit) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(watchHistoryService.getRecommendations(user, limit));
    }

    //platform trending videos endpoint
    @GetMapping("/trending")
    //retrieves platform trending videos meeting engagement threshold
    public ResponseEntity<List<VideoResponseDTO>> getPlatformTrendingVideos(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(watchHistoryService.getPlatformTrendingVideos(limit));
    }
}
