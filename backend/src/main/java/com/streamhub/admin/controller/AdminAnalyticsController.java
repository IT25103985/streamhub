package com.streamhub.admin.controller;

import com.streamhub.admin.dto.AdminAnalyticsResponseDTO;
import com.streamhub.admin.service.AdminAnalyticsService;
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
@RequestMapping({"/api/v1/admin/analytics", "/api/admin/analytics"})
@PreAuthorize("hasRole('ADMINISTRATOR')")
//serves aggregated statistics and trending media for platform admins
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;
    private final WatchHistoryService watchHistoryService;

    public AdminAnalyticsController(AdminAnalyticsService adminAnalyticsService,
                                    WatchHistoryService watchHistoryService) {
        this.adminAnalyticsService = adminAnalyticsService;
        this.watchHistoryService = watchHistoryService;
    }

    @GetMapping
    public ResponseEntity<AdminAnalyticsResponseDTO> getAnalytics() {
        return ResponseEntity.ok(adminAnalyticsService.getAnalyticsSummary());
    }

    @GetMapping("/trending")
    public ResponseEntity<List<VideoResponseDTO>> getPlatformTrendingVideos(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(watchHistoryService.getPlatformTrendingVideos(limit));
    }
}
