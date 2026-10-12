package com.streamhub.watchhistory.service;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.common.exception.ResourceNotFoundException;
import com.streamhub.settings.entity.UserSettings;
import com.streamhub.settings.service.UserSettingsService;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.watchhistory.dto.WatchHistoryResponseDTO;
import com.streamhub.watchhistory.entity.WatchHistory;
import com.streamhub.watchhistory.repository.WatchHistoryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//service tracking playback position and computing personalized recommendations
public class WatchHistoryService {

    public static final int MAX_RECOMMENDATIONS = 3;
    private static final long CYCLE_SECONDS = 7L * 24 * 3600;
    private static final ZoneId COLOMBO = ZoneId.of("Asia/Colombo");

    private final WatchHistoryRepository watchHistoryRepository;
    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final PreferredCategoryStrategy preferredCategoryStrategy;
    private final HistoryBasedStrategy historyBasedStrategy;
    private final TrendingFallbackStrategy trendingFallbackStrategy;
    private final UserSettingsService userSettingsService;
    private final Clock clock;

    public WatchHistoryService(WatchHistoryRepository watchHistoryRepository,
                               VideoRepository videoRepository,
                               UserRepository userRepository,
                               PreferredCategoryStrategy preferredCategoryStrategy,
                               HistoryBasedStrategy historyBasedStrategy,
                               TrendingFallbackStrategy trendingFallbackStrategy,
                               UserSettingsService userSettingsService,
                               Clock clock) {
        this.watchHistoryRepository = watchHistoryRepository;
        this.videoRepository = videoRepository;
        this.userRepository = userRepository;
        this.preferredCategoryStrategy = preferredCategoryStrategy;
        this.historyBasedStrategy = historyBasedStrategy;
        this.trendingFallbackStrategy = trendingFallbackStrategy;
        this.userSettingsService = userSettingsService;
        this.clock = clock;
    }

    //converts a server local datetime to the calendar date in asia/colombo
    private LocalDate toColomboDate(LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(COLOMBO)
                .toLocalDate();
    }

    //today in asia/colombo using injected clock
    private LocalDate todayColombo() {
        return LocalDate.now(clock.withZone(COLOMBO));
    }

    @Transactional
    //watch history tracking logic with per day deduplication
    public WatchHistory recordWatchHistory(User user, UUID videoId, Integer positionSeconds, UUID existingRowId) {
        if (user == null) {
            return null;
        }

        UserSettings settings = userSettingsService.getOrCreate(user.getUserId());
        if (!settings.isWatchHistoryEnabled()) {
            return null;
        }

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id " + videoId));

        int pos = positionSeconds != null ? Math.max(0, positionSeconds) : 0;

        //fast path if frontend sent existing row id
        if (existingRowId != null) {
            Optional<WatchHistory> direct = watchHistoryRepository.findById(existingRowId);
            if (direct.isPresent()) {
                WatchHistory row = direct.get();
                if (row.getUser().getUserId().equals(user.getUserId())
                        && row.getVideo().getVideoId().equals(videoId)) {
                    row.setLastPosition(pos);
                    row.setWatchDurationSeconds(pos);
                    row.setUpdatedAt(LocalDateTime.now(clock));
                    return watchHistoryRepository.save(row);
                }
            }
        }

        //pessimistic lock on user row to serialize concurrent writes
        userRepository.findByIdWithLock(user.getUserId());

        //check if user's latest watched video was this same video today
        Optional<WatchHistory> latestOverall = watchHistoryRepository.findFirstByUserOrderByUpdatedAtDesc(user);
        LocalDate today = todayColombo();

        if (latestOverall.isPresent()) {
            WatchHistory existingRow = latestOverall.get();
            if (existingRow.getVideo().getVideoId().equals(videoId) && today.equals(toColomboDate(existingRow.getUpdatedAt()))) {
                //same video consecutively on the same day: update in place and refresh timestamp
                existingRow.setLastPosition(pos);
                existingRow.setWatchDurationSeconds(pos);
                existingRow.setUpdatedAt(LocalDateTime.now(clock));
                return watchHistoryRepository.save(existingRow);
            }
        }

        //different video or different day insert new row
        WatchHistory newRow = new WatchHistory(user, video, pos);
        newRow.setWatchDurationSeconds(pos);
        newRow.setUpdatedAt(LocalDateTime.now(clock));
        return watchHistoryRepository.save(newRow);
    }

    //overload for callers that do not pass a row id
    @Transactional
    public WatchHistory recordWatchHistory(User user, UUID videoId, Integer positionSeconds) {
        return recordWatchHistory(user, videoId, positionSeconds, null);
    }

    //retrieves stored playback timestamp in seconds for a specific video
    @Transactional(readOnly = true)
    public int getResumePosition(User user, UUID videoId) {
        if (user == null || videoId == null) {
            return 0;
        }
        //read from the latest row for this user+video pair
        List<WatchHistory> rows = watchHistoryRepository
                .findLatestByUserIdAndVideoId(user.getUserId(), videoId, PageRequest.of(0, 1));
        if (rows.isEmpty()) {
            return 0;
        }
        return rows.get(0).getLastPosition();
    }

    //retrieves page of user watch history ordered by most recent
    @Transactional(readOnly = true)
    public Page<WatchHistoryResponseDTO> getUserWatchHistory(User user, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WatchHistory> historyPage = watchHistoryRepository.findByUserOrderByUpdatedAtDesc(user, pageable);
        return historyPage.map(this::mapToResponseDTO);
    }

    //deletes all playback history entries for user and records clear timestamp
    @Transactional
    public void clearUserHistory(User user) {
        if (user == null || user.getUserId() == null) {
            return;
        }
        //pessimistic lock on user row to serialize concurrent clears
        userRepository.findByIdWithLock(user.getUserId());
        watchHistoryRepository.deleteByUser(user);
        userSettingsService.recordHistoryCleared(user.getUserId(), LocalDateTime.now(clock));
    }

    //deletes single watch history row by its id
    @Transactional
    public void deleteSingleHistory(User user, UUID watchHistoryId) {
        if (user == null || watchHistoryId == null) return;
        Optional<WatchHistory> row = watchHistoryRepository.findById(watchHistoryId);
        if (row.isPresent() && row.get().getUser().getUserId().equals(user.getUserId())) {
            watchHistoryRepository.deleteById(watchHistoryId);
        }
    }

    @Transactional(readOnly = true)
    //recommends up to 3 videos based on 7 day cycle and watch history
    public List<VideoResponseDTO> getRecommendations(User user, int limit) {
        if (user == null || user.getUserId() == null || !user.isHasSetInterests()) {
            return Collections.emptyList();
        }

        LocalDateTime now = LocalDateTime.now(clock);

        //read only lookup without inserting rows
        Optional<UserSettings> settingsOpt = userSettingsService.findByUserId(user.getUserId());
        LocalDateTime clearedAt = settingsOpt.map(UserSettings::getHistoryClearedAt).orElse(null);

        //hide section for 7 days after history clear
        if (clearedAt != null && now.isBefore(clearedAt.plusDays(7))) {
            return Collections.emptyList();
        }

        //anchor cycle to cleared timestamp or account creation
        LocalDateTime baseTime = (clearedAt != null) ? clearedAt : user.getCreatedAt();
        if (baseTime == null) {
            baseTime = now;
        }

        long diffSeconds = ChronoUnit.SECONDS.between(baseTime, now);
        if (diffSeconds < 0) {
            return Collections.emptyList();
        }

        long cycleIndex = diffSeconds / CYCLE_SECONDS;
        List<Video> videos;

        if (cycleIndex == 0) {
            if (clearedAt != null) {
                return Collections.emptyList();
            }
            //first cycle uses picked categories
            videos = preferredCategoryStrategy.getRecommendations(user, MAX_RECOMMENDATIONS);
        } else {
            LocalDateTime cycleStart = baseTime.plusSeconds(cycleIndex * CYCLE_SECONDS);
            LocalDateTime windowStart = cycleStart.minusDays(7);
            LocalDateTime windowEnd = cycleStart;

            //later cycles derive top categories from watch history window
            videos = historyBasedStrategy.getRecommendations(user, MAX_RECOMMENDATIONS, windowStart, windowEnd);

            //fallback to picked categories only if user never cleared history
            if ((videos == null || videos.isEmpty()) && clearedAt == null) {
                videos = preferredCategoryStrategy.getRecommendations(user, MAX_RECOMMENDATIONS);
            }
        }

        if (videos == null || videos.isEmpty()) {
            return Collections.emptyList();
        }

        return videos.stream()
                .limit(MAX_RECOMMENDATIONS)
                .map(this::mapVideoToDTO)
                .collect(Collectors.toList());
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
