package com.streamhub.watchhistory.service;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;

import java.util.List;

//interface defining algorithm contract for video recommendations
public interface RecommendationStrategy {
    List<Video> getRecommendations(User user, int limit);
}
