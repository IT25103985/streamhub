package com.streamhub.watchhistory;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.category.entity.Category;
import com.streamhub.common.entity.User;
import com.streamhub.preferences.repository.UserCategoryPreferenceRepository;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.watchhistory.controller.WatchHistoryController;
import com.streamhub.watchhistory.repository.WatchHistoryRepository;
import com.streamhub.watchhistory.service.CategoryAffinityStrategy;
import com.streamhub.watchhistory.service.PreferredCategoryStrategy;
import com.streamhub.watchhistory.service.TrendingFallbackStrategy;
import com.streamhub.watchhistory.service.WatchHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//unit test suite for recommendation and trending engagement thresholds and auth guard
public class WatchHistoryRecommendationTest {

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private WatchHistoryRepository watchHistoryRepository;

    @Mock
    private UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.streamhub.watchhistory.repository.VideoViewEventRepository videoViewEventRepository;

    private TrendingFallbackStrategy trendingFallbackStrategy;
    private PreferredCategoryStrategy preferredCategoryStrategy;
    private CategoryAffinityStrategy categoryAffinityStrategy;
    private WatchHistoryService watchHistoryService;
    private WatchHistoryController watchHistoryController;

    private User testUser;
    private Video highViewVideo;
    private Video zeroViewVideo;
    private Category gamingCategory;

    @BeforeEach
    public void setup() {
        trendingFallbackStrategy = new TrendingFallbackStrategy(videoViewEventRepository, videoRepository);
        preferredCategoryStrategy = new PreferredCategoryStrategy(userCategoryPreferenceRepository, videoRepository, trendingFallbackStrategy);
        categoryAffinityStrategy = new CategoryAffinityStrategy(watchHistoryRepository, videoRepository);
        watchHistoryService = new WatchHistoryService(watchHistoryRepository, videoRepository, categoryAffinityStrategy, preferredCategoryStrategy, trendingFallbackStrategy);
        watchHistoryController = new WatchHistoryController(watchHistoryService, userRepository);

        testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setHasSetInterests(false);

        gamingCategory = new Category();
        gamingCategory.setId(UUID.randomUUID());
        gamingCategory.setName("Gaming");

        highViewVideo = new Video("Popular Video", "Desc", "path1.mp4", "thumb1.jpg", testUser, "PUBLIC");
        highViewVideo.setVideoId(UUID.randomUUID());
        highViewVideo.setViewCount(25);
        highViewVideo.setCategory(gamingCategory);

        zeroViewVideo = new Video("Brand New Video", "Desc", "path2.mp4", "thumb2.jpg", testUser, "PUBLIC");
        zeroViewVideo.setVideoId(UUID.randomUUID());
        zeroViewVideo.setViewCount(0);
        zeroViewVideo.setCategory(gamingCategory);
    }

    @Test
    //verifies trending fallback filters videos below engagement threshold and backfills
    public void testTrendingFallbackFiltersBelowThresholdAndBackfills() {
        when(videoViewEventRepository.findTrendingVideosWithMinWatches(any(), eq(5L), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));
        when(videoViewEventRepository.findTrendingVideosBelowMinWatches(any(), eq(5L), any(PageRequest.class)))
                .thenReturn(List.of(zeroViewVideo));

        List<Video> results = trendingFallbackStrategy.getRecommendations(null, 2);

        assertEquals(2, results.size());
        assertEquals(highViewVideo, results.get(0));
        assertEquals(zeroViewVideo, results.get(1));
        verify(videoViewEventRepository, times(1)).findTrendingVideosWithMinWatches(any(), eq(5L), any(PageRequest.class));
        verify(videoViewEventRepository, times(1)).findTrendingVideosBelowMinWatches(any(), eq(5L), any(PageRequest.class));
    }

    @Test
    //verifies category affinity applies minimum engagement threshold and backfills
    public void testCategoryAffinityFiltersBelowThresholdAndBackfills() {
        Object[] catRow = new Object[]{gamingCategory.getId(), 5L};
        List<Object[]> catList = new ArrayList<>();
        catList.add(catRow);
        when(watchHistoryRepository.findMostWatchedCategoryDataByUser(eq(testUser), any(PageRequest.class)))
                .thenReturn(catList);
        when(videoRepository.findPublicVideosByCategoryWithMinViews(eq(gamingCategory.getId()), eq(CategoryAffinityStrategy.MIN_VIEWS_FOR_TRENDING), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));
        when(videoRepository.findPublicVideosByCategoryBelowMinViews(eq(gamingCategory.getId()), eq(CategoryAffinityStrategy.MIN_VIEWS_FOR_TRENDING), any(PageRequest.class)))
                .thenReturn(List.of(zeroViewVideo));

        List<Video> results = categoryAffinityStrategy.getRecommendations(testUser, 2);

        assertEquals(2, results.size());
        assertEquals(highViewVideo, results.get(0));
        assertEquals(zeroViewVideo, results.get(1));
    }

    @Test
    //verifies cold start recommendations use preferred category strategy once preferences are set
    public void testColdStartRecommendationsUsePreferredCategories() {
        testUser.setHasSetInterests(true);
        List<UUID> preferredCatIds = List.of(gamingCategory.getId());
        when(watchHistoryRepository.countDistinctWatchedVideosByUser(eq(testUser))).thenReturn(0L);
        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser))).thenReturn(preferredCatIds);
        when(videoRepository.findPublicVideosByCategoriesWithMinViews(eq(preferredCatIds), eq(5), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));

        List<VideoResponseDTO> recs = watchHistoryService.getRecommendations(testUser, 1);

        assertEquals(1, recs.size());
        assertEquals(highViewVideo.getVideoId(), recs.get(0).getVideoId());
    }

    @Test
    //verifies user with enough watch history switches to behavioral category affinity
    public void testBehavioralWatchHistorySwitchesToCategoryAffinity() {
        when(watchHistoryRepository.countDistinctWatchedVideosByUser(eq(testUser))).thenReturn(5L);
        Object[] catRow = new Object[]{gamingCategory.getId(), 8L};
        List<Object[]> catList = new ArrayList<>();
        catList.add(catRow);
        when(watchHistoryRepository.findMostWatchedCategoryDataByUser(eq(testUser), any(PageRequest.class)))
                .thenReturn(catList);
        when(videoRepository.findPublicVideosByCategoryWithMinViews(eq(gamingCategory.getId()), eq(5), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));

        List<VideoResponseDTO> recs = watchHistoryService.getRecommendations(testUser, 1);

        assertEquals(1, recs.size());
        assertEquals(highViewVideo.getVideoId(), recs.get(0).getVideoId());
        verify(watchHistoryRepository, times(1)).findMostWatchedCategoryDataByUser(eq(testUser), any(PageRequest.class));
    }

    @Test
    //verifies fresh user who has not completed onboarding gets empty recommendations list
    public void testFreshUserWithoutOnboardingGetsEmptyRecommendations() {
        testUser.setHasSetInterests(false);
        when(watchHistoryRepository.countDistinctWatchedVideosByUser(eq(testUser))).thenReturn(0L);

        List<VideoResponseDTO> recs = watchHistoryService.getRecommendations(testUser, 10);

        assertNotNull(recs);
        assertTrue(recs.isEmpty());
        verify(videoRepository, never()).findPublicTrendingVideosWithMinViews(anyInt(), any(PageRequest.class));
    }

    @Test
    //verifies guest calling recommendations endpoint directly receives 401 unauthorized
    public void testGuestCallingRecommendationsReturns401() {
        ResponseEntity<List<VideoResponseDTO>> response = watchHistoryController.getRecommendations(null, 10);
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    //verifies public trending endpoint is accessible without authentication
    public void testPublicTrendingEndpointAccessibleForGuests() {
        when(videoViewEventRepository.findTrendingVideosWithMinWatches(any(), eq(5L), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));

        ResponseEntity<List<VideoResponseDTO>> response = watchHistoryController.getPlatformTrendingVideos(10);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        List<VideoResponseDTO> body = response.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertEquals(highViewVideo.getVideoId(), body.get(0).getVideoId());
    }
}
