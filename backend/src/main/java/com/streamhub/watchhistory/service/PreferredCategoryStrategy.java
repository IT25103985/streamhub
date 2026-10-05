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

    public static final int MIN_VIEWS_FOR_RECOMMENDATION = 0;

    private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;
    private final VideoRepository videoRepository;

    public PreferredCategoryStrategy(UserCategoryPreferenceRepository userCategoryPreferenceRepository,
                                     VideoRepository videoRepository) {
        this.userCategoryPreferenceRepository = userCategoryPreferenceRepository;
        this.videoRepository = videoRepository;
    }

    //recommends videos strictly matching user onboarding categories ordered by most viewed
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
        //query public videos across preferred categories ordered by view count desc
        List<Video> rawResults = videoRepository.findPublicVideosByCategoriesWithMinViews(
                preferredCategoryIds,
                MIN_VIEWS_FOR_RECOMMENDATION,
                PageRequest.of(0, limit)
        );
        if (rawResults != null) {
            for (Video v : rawResults) {
                if (v != null && !isOwnVideo(user, v) && results.size() < limit) {
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
