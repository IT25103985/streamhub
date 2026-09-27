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

    public static final int MIN_VIEWS_FOR_RECOMMENDATION = 10;

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

    //recommends videos matching user onboarding categories backfilling with trending
    @Override
    public List<Video> getRecommendations(User user, int limit) {
        if (user == null) {
            return new ArrayList<>();
        }

        List<UUID> preferredCategoryIds = userCategoryPreferenceRepository.findCategoryIdsByUser(user);
        if (preferredCategoryIds == null || preferredCategoryIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<Video> results = new ArrayList<>();
        //query public videos across preferred categories meeting minimum view count (>= 10)
        List<Video> rawResults = videoRepository.findPublicVideosByCategoriesWithMinViews(
                preferredCategoryIds,
                MIN_VIEWS_FOR_RECOMMENDATION,
                PageRequest.of(0, limit)
        );
        if (rawResults != null) {
            for (Video v : rawResults) {
                if (v != null && v.getViewCount() >= MIN_VIEWS_FOR_RECOMMENDATION && !isOwnVideo(user, v)) {
                    results.add(v);
                }
            }
        }

        //backfill with platform trending if qualifying preferred videos are fewer than requested limit
        if (results.size() < limit) {
            List<Video> trending = trendingFallbackStrategy.getRecommendations(user, limit);
            if (trending != null) {
                for (Video v : trending) {
                    if (!results.contains(v) && !v.isHidden() && (v.getPrivacy() == null || "PUBLIC".equalsIgnoreCase(v.getPrivacy())) 
                            && v.getViewCount() >= MIN_VIEWS_FOR_RECOMMENDATION && !isOwnVideo(user, v) && results.size() < limit) {
                        results.add(v);
                    }
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
