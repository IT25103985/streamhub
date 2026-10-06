package com.streamhub.watchhistory.service;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;

import java.util.List;

//interface defining algorithm contract for video recommendations
public interface RecommendationStrategy {
    List<Video> getRecommendations(User user, int limit);

    default List<Video> getRecommendations(User user, int limit, java.time.LocalDateTime windowStart, java.time.LocalDateTime windowEnd) {
        return getRecommendations(user, limit);
    }
}
