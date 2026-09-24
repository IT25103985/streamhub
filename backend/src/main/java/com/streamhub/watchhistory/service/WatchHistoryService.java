package com.streamhub.watchhistory.service;

import com.streamhub.common.entity.User;
import com.streamhub.common.exception.ResourceNotFoundException;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.watchhistory.dto.WatchHistoryResponseDTO;
import com.streamhub.watchhistory.entity.WatchHistory;
import com.streamhub.watchhistory.repository.WatchHistoryRepository;
import com.streamhub.settings.service.UserSettingsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//service tracking playback position and computing personalized recommendations
public class WatchHistoryService {

    //threshold of distinct videos watched before switching from onboarding preferences to behavioral affinity
    public static final int MIN_WATCHED_FOR_BEHAVIORAL = 10;

    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoRepository videoRepository;
    private final CategoryAffinityStrategy categoryAffinityStrategy;
    private final PreferredCategoryStrategy preferredCategoryStrategy;
    private final TrendingFallbackStrategy trendingFallbackStrategy;

    private final UserSettingsService userSettingsService;

    public WatchHistoryService(WatchHistoryRepository watchHistoryRepository,
                               VideoRepository videoRepository,
                               CategoryAffinityStrategy categoryAffinityStrategy,
                               PreferredCategoryStrategy preferredCategoryStrategy,
                               TrendingFallbackStrategy trendingFallbackStrategy,
                               UserSettingsService userSettingsService) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.videoRepository = videoRepository;
        this.categoryAffinityStrategy = categoryAffinityStrategy;
        this.preferredCategoryStrategy = preferredCategoryStrategy;
        this.trendingFallbackStrategy = trendingFallbackStrategy;
        this.userSettingsService = userSettingsService;
    }

    @Transactional
    //watch history tracking logic
    public void recordWatchHistory(User user, UUID videoId, Integer positionSeconds) {
        if (user != null) {
            com.streamhub.settings.entity.UserSettings settings = userSettingsService.getOrCreate(user.getUserId());
            if (!settings.isWatchHistoryEnabled()) {
                return;
            }
        }
        
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id " + videoId));

        int pos = positionSeconds != null ? Math.max(0, positionSeconds) : 0;

        WatchHistory watchHistory = watchHistoryRepository.findByUserAndVideo(user, video)
                .orElseGet(() -> new WatchHistory(user, video, pos));

        watchHistory.setLastPosition(pos);
        watchHistory.setWatchDurationSeconds(pos);
        watchHistoryRepository.save(watchHistory);
    }

    @Transactional(readOnly = true)
    public int getResumePosition(User user, UUID videoId) {
        if (user == null || videoId == null) {
            return 0;
        }
        return watchHistoryRepository.findByUser_UserIdAndVideo_VideoId(user.getUserId(), videoId)
                .map(WatchHistory::getLastPosition)
                .orElse(0);
    }

    @Transactional(readOnly = true)
    public Page<WatchHistoryResponseDTO> getUserWatchHistory(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchHistory> historyPage = watchHistoryRepository.findByUserOrderByUpdatedAtDesc(user, pageable);
        
        return historyPage.map(this::mapToResponseDTO);
    }

    @Transactional
    public void clearUserHistory(User user) {
        if (user != null) {
            watchHistoryRepository.deleteByUser(user);
        }
    }

    @Transactional
    public void deleteSingleHistory(User user, UUID videoId) {
        if (user != null && videoId != null) {
            watchHistoryRepository.deleteByUserAndVideo_VideoId(user, videoId);
        }
    }

    @Transactional(readOnly = true)
    //recommendation calculation with behavioral preferred and trending strategy order
    public List<VideoResponseDTO> getRecommendations(User user, int limit) {
        if (user == null) {
            return new java.util.ArrayList<>();
        }

        long watchedCount = watchHistoryRepository.countDistinctWatchedVideosByUser(user);
        //gate fresh accounts with neither onboarding preferences nor behavioral history
        if (!user.isHasSetInterests() && watchedCount < MIN_WATCHED_FOR_BEHAVIORAL) {
            return new java.util.ArrayList<>();
        }

        List<Video> recommendations = new java.util.ArrayList<>();
        
        List<Video> primary = (watchedCount >= MIN_WATCHED_FOR_BEHAVIORAL)
                ? categoryAffinityStrategy.getRecommendations(user, limit)
                : preferredCategoryStrategy.getRecommendations(user, limit);

        if (primary != null) {
            for (Video v : primary) {
                if (isValidRecommendation(v, user) && !recommendations.contains(v) && recommendations.size() < limit) {
                    recommendations.add(v);
                }
            }
        }
        
        //fallback to trending if slots remaining after preferred categories or behavioral affinity
        if (recommendations.size() < limit) {
            List<Video> trending = trendingFallbackStrategy.getRecommendations(user, limit);
            if (trending != null) {
                for (Video v : trending) {
                    if (isValidRecommendation(v, user) && !recommendations.contains(v) && recommendations.size() < limit) {
                        recommendations.add(v);
                    }
                }
            }
        }
        
        return recommendations.stream()
                .filter(v -> isValidRecommendation(v, user))
                .map(this::mapVideoToDTO)
                .collect(Collectors.toList());
    }

    private boolean isValidRecommendation(Video video, User user) {
        if (video == null || video.isHidden()) {
            return false;
        }
        if (video.getPrivacy() != null && !"PUBLIC".equalsIgnoreCase(video.getPrivacy())) {
            return false;
        }
        // Recommended videos must have viewCount >= 10
        if (video.getViewCount() < 10) {
            return false;
        }
        // Exclude user's own videos from recommendations
        if (user != null && video.getOwner() != null) {
            if (user.getUserId() != null && user.getUserId().equals(video.getOwner().getUserId())) {
                return false;
            }
            if (user.getUsername() != null && user.getUsername().equalsIgnoreCase(video.getOwner().getUsername())) {
                return false;
            }
        }
        return true;
    }

    @Transactional(readOnly = true)
    //platform trending aggregation logic
    public List<VideoResponseDTO> getPlatformTrendingVideos(int limit) {
        List<Video> trending = trendingFallbackStrategy.getRecommendations(null, limit);
        return trending.stream()
                .filter(v -> v != null && !v.isHidden() && (v.getPrivacy() == null || "PUBLIC".equalsIgnoreCase(v.getPrivacy())) && v.getViewCount() >= 10)
                .map(this::mapVideoToDTO)
                .collect(Collectors.toList());
    }

    private WatchHistoryResponseDTO mapToResponseDTO(WatchHistory history) {
        return new WatchHistoryResponseDTO(
                history.getId(),
                mapVideoToDTO(history.getVideo()),
                history.getLastPosition(),
                history.getWatchedAt(),
                history.getUpdatedAt()
        );
    }

    private VideoResponseDTO mapVideoToDTO(Video video) {
        VideoResponseDTO dto = new VideoResponseDTO();
        dto.setVideoId(video.getVideoId());
        dto.setTitle(video.getTitle());
        dto.setDescription(video.getDescription());
        dto.setThumbnailUrl(video.getThumbnailUrl());
        if (video.getOwner() != null) {
            dto.setOwnerId(video.getOwner().getUserId());
            dto.setOwnerUsername(video.getOwner().getUsername());
            dto.setOwnerAvatarUrl(video.getOwner().getAvatarUrl());
        }
        dto.setCreatedAt(video.getCreatedAt());
        dto.setViewCount(video.getViewCount());
        dto.setHidden(video.isHidden());
        dto.setPrivacy(video.getPrivacy());
        return dto;
    }
}
