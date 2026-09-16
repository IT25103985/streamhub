package com.streamhub.watchhistory.service;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.watchhistory.repository.VideoViewEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
//fallback strategy returning overall popular videos based on rolling 7-day watch volume
public class TrendingFallbackStrategy implements RecommendationStrategy {

    //minimum recent watch count threshold required for trending qualification
    public static final int MIN_RECENT_WATCHES_FOR_TRENDING = 5;

    //minimum view count required for video recommendation (at least 10 views)
    public static final int MIN_VIEWS_FOR_TRENDING = 10;

    private final VideoViewEventRepository videoViewEventRepository;
    private final VideoRepository videoRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public TrendingFallbackStrategy(VideoViewEventRepository videoViewEventRepository, VideoRepository videoRepository) {
        this.videoViewEventRepository = videoViewEventRepository;
        this.videoRepository = videoRepository;
    }

    public TrendingFallbackStrategy(VideoRepository videoRepository) {
        this(null, videoRepository);
    }

    @Override
    public List<Video> getRecommendations(User user, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        Set<UUID> addedVideoIds = new HashSet<>();
        List<Video> results = new ArrayList<>();

        if (videoViewEventRepository != null) {
            //query public videos meeting rolling 7-day watch threshold
            List<Video> qualifying = videoViewEventRepository.findTrendingVideosWithMinWatches(
                    since,
                    MIN_RECENT_WATCHES_FOR_TRENDING,
                    PageRequest.of(0, limit)
            );
            if (qualifying != null) {
                for (Video v : qualifying) {
                    if (results.size() < limit && v != null && v.getVideoId() != null 
                            && v.getViewCount() >= MIN_VIEWS_FOR_TRENDING
                            && !isOwnVideo(user, v) && addedVideoIds.add(v.getVideoId())) {
                        results.add(v);
                    }
                }
            }
        }

        //backfill with public non-hidden videos meeting min view count
        if (results.size() < limit && videoRepository != null) {
            List<Video> allPublic = videoRepository.findPublicTrendingVideosWithMinViews(MIN_VIEWS_FOR_TRENDING, PageRequest.of(0, limit));
            if (allPublic != null) {
                for (Video v : allPublic) {
                    if (results.size() >= limit) break;
                    if (v != null && v.getVideoId() != null && v.getViewCount() >= MIN_VIEWS_FOR_TRENDING && !isOwnVideo(user, v) && addedVideoIds.add(v.getVideoId())) {
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
