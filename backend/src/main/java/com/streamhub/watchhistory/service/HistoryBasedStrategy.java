package com.streamhub.watchhistory.service;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.watchhistory.repository.WatchHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//recommendation strategy deriving top categories from watch history window
public class HistoryBasedStrategy implements RecommendationStrategy {

    public static final int MIN_VIEWS_FOR_RECOMMENDATION = 10;
    public static final int MAX_RECOMMENDATIONS = 3;
    public static final int MAX_CATEGORIES = 4;

    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoRepository videoRepository;

    public HistoryBasedStrategy(WatchHistoryRepository watchHistoryRepository, VideoRepository videoRepository) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.videoRepository = videoRepository;
    }

    @Override
    public List<Video> getRecommendations(User user, int limit) {
        //default window of previous 7 days when window bounds omitted
        LocalDateTime now = LocalDateTime.now();
        return getRecommendations(user, limit, now.minusDays(7), now);
    }

    @Override
    public List<Video> getRecommendations(User user, int limit, LocalDateTime windowStart, LocalDateTime windowEnd) {
        if (user == null || user.getUserId() == null || windowStart == null || windowEnd == null) {
            return new ArrayList<>();
        }

        //query top 1 to 4 categories from watch history in window
        List<Object[]> topCategories = watchHistoryRepository.findTopCategoryDataByUserInWindow(
                user,
                windowStart,
                windowEnd,
                PageRequest.of(0, MAX_CATEGORIES)
        );

        if (topCategories == null || topCategories.isEmpty()) {
            return new ArrayList<>();
        }

        List<UUID> categoryIds = topCategories.stream()
                .map(row -> (UUID) row[0])
                .collect(Collectors.toList());

        int effectiveLimit = Math.min(Math.max(1, limit), MAX_RECOMMENDATIONS);

        return videoRepository.findEligibleVideosByCategories(
                categoryIds,
                user.getUserId(),
                MIN_VIEWS_FOR_RECOMMENDATION,
                PageRequest.of(0, effectiveLimit)
        );
    }
}
