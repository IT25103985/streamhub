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

    //minimum view count threshold required for category recommendation qualification
    public static final int MIN_VIEWS_FOR_TRENDING = 5;

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
        
        //query category videos meeting engagement threshold
        List<Video> results = new ArrayList<>(videoRepository.findPublicVideosByCategoryWithMinViews(topCategoryId, MIN_VIEWS_FOR_TRENDING, PageRequest.of(0, limit)));
        if (results.size() < limit) {
            //backfill remaining slots with lower viewed category videos
            int remaining = limit - results.size();
            List<Video> backfill = videoRepository.findPublicVideosByCategoryBelowMinViews(topCategoryId, MIN_VIEWS_FOR_TRENDING, PageRequest.of(0, remaining));
            results.addAll(backfill);
        }
        return results;
    }
}

