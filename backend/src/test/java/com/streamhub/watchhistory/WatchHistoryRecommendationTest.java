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

import java.util.ArrayList;
import java.util.List;
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
    private User otherUser;
    private Video highViewVideo;
    private Video qualifyingSecondVideo;
    private Video userOwnVideo;
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

        otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setUsername("otheruser");

        gamingCategory = new Category();
        gamingCategory.setId(UUID.randomUUID());
        gamingCategory.setName("Gaming");

        highViewVideo = new Video("Popular Video", "Desc", "path1.mp4", "thumb1.jpg", otherUser, "PUBLIC");
        highViewVideo.setVideoId(UUID.randomUUID());
        highViewVideo.setViewCount(25);
        highViewVideo.setCategory(gamingCategory);

        qualifyingSecondVideo = new Video("Second Video", "Desc", "path2.mp4", "thumb2.jpg", otherUser, "PUBLIC");
        qualifyingSecondVideo.setVideoId(UUID.randomUUID());
        qualifyingSecondVideo.setViewCount(12);
        qualifyingSecondVideo.setCategory(gamingCategory);

        userOwnVideo = new Video("My Own Video", "Desc", "path3.mp4", "thumb3.jpg", testUser, "PUBLIC");
        userOwnVideo.setVideoId(UUID.randomUUID());
        userOwnVideo.setViewCount(100);
        userOwnVideo.setCategory(gamingCategory);
    }

    @Test
    //verifies trending fallback filters videos below engagement threshold and returns qualifying videos
    public void testTrendingFallbackFiltersBelowThresholdAndBackfills() {
        when(videoViewEventRepository.findTrendingVideosWithMinWatches(any(), eq(5L), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));
        when(videoRepository.findPublicTrendingVideosWithMinViews(eq(10), any(PageRequest.class)))
                .thenReturn(List.of(qualifyingSecondVideo));

        List<Video> results = trendingFallbackStrategy.getRecommendations(null, 2);

        assertEquals(2, results.size());
        assertEquals(highViewVideo, results.get(0));
        assertEquals(qualifyingSecondVideo, results.get(1));
    }

    @Test
    //verifies category affinity applies minimum engagement threshold (viewCount >= 10)
    public void testCategoryAffinityFiltersBelowThresholdAndBackfills() {
        Object[] catRow = new Object[]{gamingCategory.getId(), 5L};
        List<Object[]> catList = new ArrayList<>();
        catList.add(catRow);
        when(watchHistoryRepository.findMostWatchedCategoryDataByUser(eq(testUser), any(PageRequest.class)))
                .thenReturn(catList);
        when(videoRepository.findPublicVideosByCategoryWithMinViews(eq(gamingCategory.getId()), eq(CategoryAffinityStrategy.MIN_VIEWS_FOR_TRENDING), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo, qualifyingSecondVideo));

        List<Video> results = categoryAffinityStrategy.getRecommendations(testUser, 2);

        assertEquals(2, results.size());
        assertEquals(highViewVideo, results.get(0));
        assertEquals(qualifyingSecondVideo, results.get(1));
    }

    @Test
    //verifies cold start recommendations use preferred category strategy once preferences are set
    public void testColdStartRecommendationsUsePreferredCategories() {
        testUser.setHasSetInterests(true);
        List<UUID> preferredCatIds = List.of(gamingCategory.getId());
        when(watchHistoryRepository.countDistinctWatchedVideosByUser(eq(testUser))).thenReturn(0L);
        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser))).thenReturn(preferredCatIds);
        when(videoRepository.findPublicVideosByCategoriesWithMinViews(eq(preferredCatIds), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));

        List<VideoResponseDTO> recs = watchHistoryService.getRecommendations(testUser, 1);

        assertEquals(1, recs.size());
        assertEquals(highViewVideo.getVideoId(), recs.get(0).getVideoId());
    }

    @Test
    //verifies user with enough watch history (>= 10) switches to behavioral category affinity
    public void testBehavioralWatchHistorySwitchesToCategoryAffinity() {
        when(watchHistoryRepository.countDistinctWatchedVideosByUser(eq(testUser))).thenReturn(10L);
        Object[] catRow = new Object[]{gamingCategory.getId(), 8L};
        List<Object[]> catList = new ArrayList<>();
        catList.add(catRow);
        when(watchHistoryRepository.findMostWatchedCategoryDataByUser(eq(testUser), any(PageRequest.class)))
                .thenReturn(catList);
        when(videoRepository.findPublicVideosByCategoryWithMinViews(eq(gamingCategory.getId()), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));

        List<VideoResponseDTO> recs = watchHistoryService.getRecommendations(testUser, 1);

        assertEquals(1, recs.size());
        assertEquals(highViewVideo.getVideoId(), recs.get(0).getVideoId());
        verify(watchHistoryRepository, times(1)).findMostWatchedCategoryDataByUser(eq(testUser), any(PageRequest.class));
    }

    @Test
    //verifies user with fewer than 10 watched videos still uses preferred categories
    public void testWatchCountBelowTenUsesPreferredCategories() {
        testUser.setHasSetInterests(true);
        List<UUID> preferredCatIds = List.of(gamingCategory.getId());
        when(watchHistoryRepository.countDistinctWatchedVideosByUser(eq(testUser))).thenReturn(9L);
        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser))).thenReturn(preferredCatIds);
        when(videoRepository.findPublicVideosByCategoriesWithMinViews(eq(preferredCatIds), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(highViewVideo));

        List<VideoResponseDTO> recs = watchHistoryService.getRecommendations(testUser, 1);

        assertEquals(1, recs.size());
        assertEquals(highViewVideo.getVideoId(), recs.get(0).getVideoId());
        verify(watchHistoryRepository, never()).findMostWatchedCategoryDataByUser(any(), any());
    }

    @Test
    //verifies recommended videos do not include user's own videos
    public void testRecommendationsExcludeUserOwnVideos() {
        testUser.setHasSetInterests(true);
        List<UUID> preferredCatIds = List.of(gamingCategory.getId());
        when(watchHistoryRepository.countDistinctWatchedVideosByUser(eq(testUser))).thenReturn(0L);
        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser))).thenReturn(preferredCatIds);
        // Repository returns user's own video and another user's video
        when(videoRepository.findPublicVideosByCategoriesWithMinViews(eq(preferredCatIds), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(userOwnVideo, highViewVideo));

        List<VideoResponseDTO> recs = watchHistoryService.getRecommendations(testUser, 2);

        assertEquals(1, recs.size());
        assertEquals(highViewVideo.getVideoId(), recs.get(0).getVideoId());
        assertFalse(recs.stream().anyMatch(dto -> dto.getVideoId().equals(userOwnVideo.getVideoId())));
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
