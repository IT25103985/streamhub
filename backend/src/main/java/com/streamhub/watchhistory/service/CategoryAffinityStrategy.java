package com.streamhub.watchhistory.service;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.watchhistory.repository.WatchHistoryRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
//recommends unwatched videos from categories the user watches most
public class CategoryAffinityStrategy implements RecommendationStrategy {

    //minimum view count threshold required for category recommendation qualification (at least 10 views)
    public static final int MIN_VIEWS_FOR_TRENDING = 10;

    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoRepository videoRepository;

    public CategoryAffinityStrategy(WatchHistoryRepository watchHistoryRepository, VideoRepository videoRepository) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.videoRepository = videoRepository;
    }

    @Override
    public List<Video> getRecommendations(User user, int limit) {
        List<Object[]> categoryData = watchHistoryRepository.findMostWatchedCategoryDataByUser(user, PageRequest.of(0, 1));
        if (categoryData == null || categoryData.isEmpty()) {
            return new ArrayList<>();
        }

        UUID topCategoryId = (UUID) categoryData.get(0)[0];
        
        List<Video> results = new ArrayList<>();
        //query category videos meeting engagement threshold (view count >= 10)
        List<Video> rawResults = videoRepository.findPublicVideosByCategoryWithMinViews(topCategoryId, MIN_VIEWS_FOR_TRENDING, PageRequest.of(0, limit));
        if (rawResults != null) {
            for (Video v : rawResults) {
                if (v != null && v.getViewCount() >= MIN_VIEWS_FOR_TRENDING && !isOwnVideo(user, v)) {
                    results.add(v);
                }
            }
        }
        return results;
    }

    private boolean isOwnVideo(User user, Video video) {
        if (user == null || video == null) return false;
        if (video.getOwner() == null) return false;
        if (user.getUserId() != null && user.getUserId().equals(video.getOwner().getUserId())) return true;
        if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(video.getOwner().getUsername())) return true;
        return false;
    }
}

