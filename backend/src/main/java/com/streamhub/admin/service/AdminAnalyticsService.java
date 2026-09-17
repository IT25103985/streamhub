package com.streamhub.admin.service;

import com.streamhub.admin.dto.AdminAnalyticsResponseDTO;
import com.streamhub.auth.service.AuthService;
import com.streamhub.category.service.CategoryService;
import com.streamhub.report.service.ReportService;
import com.streamhub.social.service.SocialService;
import com.streamhub.video.service.VideoService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
//aggregates system wide counts and signup metrics from domain services
public class AdminAnalyticsService {

    private final AuthService authService;
    private final VideoService videoService;
    private final CategoryService categoryService;
    private final ReportService reportService;
    private final SocialService socialService;

    public AdminAnalyticsService(AuthService authService,
                                 VideoService videoService,
                                 CategoryService categoryService,
                                 ReportService reportService,
                                 SocialService socialService) {
        this.authService = authService;
        this.videoService = videoService;
        this.categoryService = categoryService;
        this.reportService = reportService;
        this.socialService = socialService;
    }

    public AdminAnalyticsResponseDTO getAnalyticsSummary() {
        long totalUsers = authService.countAll();
        long totalVideos = videoService.countAll();
        long totalCategories = categoryService.countAll();
        long pendingReports = reportService.countByStatus("pending");
        long totalComments = socialService.countComments();
        long totalLikes = socialService.countLikes();
        long totalCommentsAndLikes = totalComments + totalLikes;

        List<Map<String, Object>> signupsRaw = authService.countSignupsByDay(7);
        List<AdminAnalyticsResponseDTO.SignupDayDTO> signupsLast7Days = signupsRaw.stream()
                .map(m -> new AdminAnalyticsResponseDTO.SignupDayDTO(
                        (String) m.get("date"),
                        ((Number) m.get("count")).longValue()
                ))
                .collect(Collectors.toList());

        return new AdminAnalyticsResponseDTO(
                totalUsers,
                totalVideos,
                totalCategories,
                pendingReports,
                totalCommentsAndLikes,
                signupsLast7Days
        );
    }
}
