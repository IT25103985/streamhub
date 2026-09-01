package com.streamhub.watchhistory;

import com.streamhub.category.repository.CategoryRepository;
import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.video.service.VideoService;
import com.streamhub.watchhistory.entity.VideoViewEvent;
import com.streamhub.watchhistory.repository.VideoViewEventRepository;
import com.streamhub.watchhistory.service.TrendingFallbackStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//unit test suite for rolling 7-day watch volume trending calculations and view event logging
public class TrendingWatchEventVolumeTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private VideoViewEventRepository videoViewEventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private VideoService videoService;
    private TrendingFallbackStrategy trendingFallbackStrategy;

    private User testUser;
    private Video testVideo;
    private Video privateVideo;
    private Video hiddenVideo;

    @BeforeEach
    public void setup() {
        videoService = new VideoService(
                videoRepository,
                categoryRepository,
                "uploads",
                null,
                null,
                videoViewEventRepository
        );

        trendingFallbackStrategy = new TrendingFallbackStrategy(videoViewEventRepository, videoRepository);

        testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        testUser.setUsername("viewer");

        testVideo = new Video("Popular Clip", "Desc", "path.mp4", "thumb.jpg", testUser, "PUBLIC");
        testVideo.setVideoId(UUID.randomUUID());
        testVideo.setViewCount(100);

        privateVideo = new Video("Private Clip", "Desc", "path2.mp4", "thumb2.jpg", testUser, "PRIVATE");
        privateVideo.setVideoId(UUID.randomUUID());
        privateVideo.setViewCount(200);

        hiddenVideo = new Video("Hidden Clip", "Desc", "path3.mp4", "thumb3.jpg", testUser, "PUBLIC");
        hiddenVideo.setVideoId(UUID.randomUUID());
        hiddenVideo.setHidden(true);
        hiddenVideo.setViewCount(300);
    }

    @Test
    //verifies repeated watch starts accumulate multiple view events and increment all-time count
    public void testRepeatedWatchesAccumulateMultipleEventsAndIncrementViewCount() {
        when(videoRepository.findById(testVideo.getVideoId())).thenReturn(Optional.of(testVideo));

        int initialViewCount = testVideo.getViewCount();

        //simulate 3 consecutive playback starts from the same user
        videoService.incrementViewCount(testVideo.getVideoId(), testUser);
        videoService.incrementViewCount(testVideo.getVideoId(), testUser);
        videoService.incrementViewCount(testVideo.getVideoId(), testUser);

        //all-time view count incremented by 3
        assertEquals(initialViewCount + 3, testVideo.getViewCount());

        //captures 3 distinct VideoViewEvent rows saved
        ArgumentCaptor<VideoViewEvent> eventCaptor = ArgumentCaptor.forClass(VideoViewEvent.class);
        verify(videoViewEventRepository, times(3)).save(eventCaptor.capture());

        List<VideoViewEvent> savedEvents = eventCaptor.getAllValues();
        assertEquals(3, savedEvents.size());
        for (VideoViewEvent event : savedEvents) {
            assertEquals(testVideo, event.getVideo());
            assertEquals(testUser, event.getUser());
            assertNotNull(event.getViewedAt());
        }
    }

    @Test
    //verifies guest watch events log video view event with null user and increments view count
    public void testGuestWatchLogsViewEventWithNullUser() {
        when(videoRepository.findById(testVideo.getVideoId())).thenReturn(Optional.of(testVideo));

        videoService.incrementViewCount(testVideo.getVideoId(), null);

        assertEquals(101, testVideo.getViewCount());

        ArgumentCaptor<VideoViewEvent> eventCaptor = ArgumentCaptor.forClass(VideoViewEvent.class);
        verify(videoViewEventRepository, times(1)).save(eventCaptor.capture());

        VideoViewEvent savedEvent = eventCaptor.getValue();
        assertEquals(testVideo, savedEvent.getVideo());
        assertNull(savedEvent.getUser());
    }

    @Test
    //verifies rolling window query filters with 7-day cutoff timestamp
    public void testTrendingRankingUsesRolling7DayCutoff() {
        when(videoViewEventRepository.findTrendingVideosWithMinWatches(any(LocalDateTime.class), eq(5L), any(PageRequest.class)))
                .thenReturn(List.of(testVideo));

        List<Video> results = trendingFallbackStrategy.getRecommendations(null, 4);

        assertEquals(1, results.size());
        assertEquals(testVideo, results.get(0));

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(videoViewEventRepository, times(1)).findTrendingVideosWithMinWatches(cutoffCaptor.capture(), eq(5L), any(PageRequest.class));

        LocalDateTime capturedCutoff = cutoffCaptor.getValue();
        LocalDateTime expectedApproxCutoff = LocalDateTime.now().minusDays(7);

        //verifies cutoff timestamp is within 5 seconds of 7 days ago
        long diffSeconds = java.time.Duration.between(capturedCutoff, expectedApproxCutoff).abs().getSeconds();
        assertTrue(diffSeconds < 5);
    }

    @Test
    //verifies private and hidden videos are excluded from trending and backfill respects filters
    public void testPrivateAndHiddenVideosExcludedFromTrending() {
        //simulate repository returning only public non-hidden qualifying videos
        when(videoViewEventRepository.findTrendingVideosWithMinWatches(any(), eq(5L), any(PageRequest.class)))
                .thenReturn(List.of(testVideo));

        List<Video> results = trendingFallbackStrategy.getRecommendations(null, 10);

        assertFalse(results.contains(privateVideo));
        assertFalse(results.contains(hiddenVideo));
        assertTrue(results.contains(testVideo));
    }

    @Test
    //verifies all-time view count on video entity remains separate from rolling trending ranking
    public void testAllTimeViewCountUnaffectedByRollingRanking() {
        testVideo.setViewCount(42);

        //rolling watch count query returns video
        when(videoViewEventRepository.findTrendingVideosWithMinWatches(any(), eq(5L), any(PageRequest.class)))
                .thenReturn(List.of(testVideo));

        List<Video> results = trendingFallbackStrategy.getRecommendations(null, 1);

        assertEquals(1, results.size());
        assertEquals(42, results.get(0).getViewCount());
    }
}
