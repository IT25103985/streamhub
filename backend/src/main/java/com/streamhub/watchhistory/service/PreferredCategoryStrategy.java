package com.streamhub.watchhistory.service;

import com.streamhub.common.entity.User;
import com.streamhub.preferences.repository.UserCategoryPreferenceRepository;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
//cold start recommendation strategy using onboarding category preferences
public class PreferredCategoryStrategy implements RecommendationStrategy {

    private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;
    private final VideoRepository videoRepository;
    private final TrendingFallbackStrategy trendingFallbackStrategy;

    public PreferredCategoryStrategy(UserCategoryPreferenceRepository userCategoryPreferenceRepository,
                                     VideoRepository videoRepository,
                                     TrendingFallbackStrategy trendingFallbackStrategy) {
        this.userCategoryPreferenceRepository = userCategoryPreferenceRepository;
        this.videoRepository = videoRepository;
        this.trendingFallbackStrategy = trendingFallbackStrategy;
    }

    @Override
    public List<Video> getRecommendations(User user, int limit) {
        if (user == null) {
            return new ArrayList<>();
        }

        List<UUID> preferredCategoryIds = userCategoryPreferenceRepository.findCategoryIdsByUser(user);
        if (preferredCategoryIds == null || preferredCategoryIds.isEmpty()) {
            return new ArrayList<>();
        }

        //query public videos across preferred categories meeting minimum view count
        List<Video> results = new ArrayList<>(videoRepository.findPublicVideosByCategoriesWithMinViews(
                preferredCategoryIds,
                TrendingFallbackStrategy.MIN_VIEWS_FOR_TRENDING,
                PageRequest.of(0, limit)
        ));

        //backfill with platform trending if qualifying preferred videos are fewer than requested limit
        if (results.size() < limit) {
            List<Video> trending = trendingFallbackStrategy.getRecommendations(user, limit);
            for (Video v : trending) {
                if (!results.contains(v) && !v.isHidden() && (v.getPrivacy() == null || "PUBLIC".equalsIgnoreCase(v.getPrivacy())) && results.size() < limit) {
                    results.add(v);
                }
            }
        }

        return results;
    }
}
