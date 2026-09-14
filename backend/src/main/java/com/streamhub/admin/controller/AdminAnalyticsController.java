package com.streamhub.admin.controller;

import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.watchhistory.service.WatchHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
@PreAuthorize("hasRole('ADMINISTRATOR')")
//serves summary metrics and system stats for the admin dashboard
public class AdminAnalyticsController {

    private final WatchHistoryService watchHistoryService;

    public AdminAnalyticsController(WatchHistoryService watchHistoryService) {
        this.watchHistoryService = watchHistoryService;
    }

    @GetMapping("/trending")
    public ResponseEntity<List<VideoResponseDTO>> getPlatformTrendingVideos(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(watchHistoryService.getPlatformTrendingVideos(limit));
    }
}
