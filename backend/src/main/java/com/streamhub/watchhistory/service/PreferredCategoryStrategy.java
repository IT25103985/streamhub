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
//recommendation strategy querying eligible videos for onboarding categories
public class PreferredCategoryStrategy implements RecommendationStrategy {

    public static final int MIN_VIEWS_FOR_RECOMMENDATION = 10;
    public static final int MAX_RECOMMENDATIONS = 3;

    private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;
    private final VideoRepository videoRepository;

    public PreferredCategoryStrategy(UserCategoryPreferenceRepository userCategoryPreferenceRepository,
                                     VideoRepository videoRepository) {
        this.userCategoryPreferenceRepository = userCategoryPreferenceRepository;
        this.videoRepository = videoRepository;
    }

    //queries eligible videos matching preferred categories ordered by view count descending
    @Override
    public List<Video> getRecommendations(User user, int limit) {
        if (user == null || user.getUserId() == null) {
            return new ArrayList<>();
        }

        List<UUID> preferredCategoryIds = userCategoryPreferenceRepository.findCategoryIdsByUser(user);
        if (preferredCategoryIds == null || preferredCategoryIds.isEmpty()) {
            return new ArrayList<>();
        }

        int effectiveLimit = Math.min(Math.max(1, limit), MAX_RECOMMENDATIONS);
        return videoRepository.findEligibleVideosByCategories(
                preferredCategoryIds,
                user.getUserId(),
                MIN_VIEWS_FOR_RECOMMENDATION,
                PageRequest.of(0, effectiveLimit)
        );
    }
}
