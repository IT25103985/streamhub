package com.streamhub.watchhistory;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.category.entity.Category;
import com.streamhub.common.entity.User;
import com.streamhub.preferences.repository.UserCategoryPreferenceRepository;
import com.streamhub.settings.entity.UserSettings;
import com.streamhub.settings.service.UserSettingsService;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import com.streamhub.watchhistory.controller.WatchHistoryController;
import com.streamhub.watchhistory.repository.WatchHistoryRepository;
import com.streamhub.watchhistory.service.HistoryBasedStrategy;
import com.streamhub.watchhistory.service.PreferredCategoryStrategy;
import com.streamhub.watchhistory.service.TrendingFallbackStrategy;
import com.streamhub.watchhistory.service.WatchHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//unit test suite for recommendation cycle windowing auth guard and history clear rules
public class WatchHistoryRecommendationTest {

    private static final ZoneId ZONE = ZoneId.of("UTC");
    private static final Instant NOW_INSTANT = Instant.parse("2026-10-06T12:00:00Z");

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

    private TestUserSettingsService testUserSettingsService;
    private Clock fixedClock;
    private PreferredCategoryStrategy preferredCategoryStrategy;
    private HistoryBasedStrategy historyBasedStrategy;
    private TrendingFallbackStrategy trendingFallbackStrategy;
    private WatchHistoryService watchHistoryService;
    private WatchHistoryController watchHistoryController;

    private User testUser;
    private User otherUser;
    private Category gamingCategory;
    private Category musicCategory;
    private Video video1;
    private Video video2;
    private Video video3;
    private Video video4;

    static class TestUserSettingsService extends UserSettingsService {
        private UserSettings userSettings;
        public boolean recordHistoryClearedCalled = false;
        public int findByUserIdCallCount = 0;
        public int getOrCreateCallCount = 0;

        public TestUserSettingsService() {
            super(null, null);
        }

        public void setUserSettings(UserSettings settings) {
            this.userSettings = settings;
        }

        @Override
        public Optional<UserSettings> findByUserId(UUID userId) {
            findByUserIdCallCount++;
            return Optional.ofNullable(userSettings);
        }

        @Override
        public UserSettings getOrCreate(UUID userId) {
            getOrCreateCallCount++;
            if (userSettings != null) return userSettings;
            userSettings = new UserSettings();
            return userSettings;
        }

        @Override
        public UserSettings recordHistoryCleared(UUID userId, LocalDateTime clearedAt) {
            recordHistoryClearedCalled = true;
            if (userSettings == null) {
                userSettings = new UserSettings();
            }
            userSettings.setHistoryClearedAt(clearedAt);
            return userSettings;
        }
    }

    @BeforeEach
    public void setup() {
        fixedClock = Clock.fixed(NOW_INSTANT, ZONE);
        testUserSettingsService = new TestUserSettingsService();

        preferredCategoryStrategy = new PreferredCategoryStrategy(userCategoryPreferenceRepository, videoRepository);
        historyBasedStrategy = new HistoryBasedStrategy(watchHistoryRepository, videoRepository);
        trendingFallbackStrategy = new TrendingFallbackStrategy(videoViewEventRepository, videoRepository);

        watchHistoryService = new WatchHistoryService(
                watchHistoryRepository,
                videoRepository,
                userRepository,
                preferredCategoryStrategy,
                historyBasedStrategy,
                trendingFallbackStrategy,
                testUserSettingsService,
                fixedClock
        );
        watchHistoryController = new WatchHistoryController(watchHistoryService, userRepository);

        testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setHasSetInterests(true);
        testUser.setCreatedAt(LocalDateTime.now(fixedClock).minusDays(20));

        otherUser = new User();
        otherUser.setUserId(UUID.randomUUID());
        otherUser.setUsername("otheruser");

        gamingCategory = new Category();
        gamingCategory.setId(UUID.randomUUID());
        gamingCategory.setName("Gaming");

        musicCategory = new Category();
        musicCategory.setId(UUID.randomUUID());
        musicCategory.setName("Music");

        video1 = createVideo("Video 1", 50, gamingCategory);
        video2 = createVideo("Video 2", 30, gamingCategory);
        video3 = createVideo("Video 3", 20, gamingCategory);
        video4 = createVideo("Video 4", 15, gamingCategory);
    }

    private Video createVideo(String title, int views, Category cat) {
        Video v = new Video(title, "Desc", "path.mp4", "thumb.jpg", otherUser, "PUBLIC");
        v.setVideoId(UUID.randomUUID());
        v.setViewCount(views);
        v.setCategory(cat);
        return v;
    }

    @Test
    //verifies recommendations hidden for 7 days after watch history clear
    public void testHiddenForSevenDaysAfterClear() {
        UserSettings settings = new UserSettings();
        //cleared 3 days ago
        settings.setHistoryClearedAt(LocalDateTime.now(fixedClock).minusDays(3));
        testUserSettingsService.setUserSettings(settings);

        List<VideoResponseDTO> results = watchHistoryService.getRecommendations(testUser, 3);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        verifyNoInteractions(videoRepository);
    }

    @Test
    //verifies recommendations remain hidden after 7 days if user has no watch history in window without fallback
    public void testHiddenAfterSevenDaysIfNoNewHistoryWithoutFallback() {
        UserSettings settings = new UserSettings();
        //cleared 10 days ago (cycle 1 after clear)
        settings.setHistoryClearedAt(LocalDateTime.now(fixedClock).minusDays(10));
        testUserSettingsService.setUserSettings(settings);

        //no watch history in the 7-day window prior to cycle 1
        when(watchHistoryRepository.findTopCategoryDataByUserInWindow(eq(testUser), any(), any(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        List<VideoResponseDTO> results = watchHistoryService.getRecommendations(testUser, 3);

        assertNotNull(results);
        assertTrue(results.isEmpty());
        //must NOT fall back to picked categories
        verify(userCategoryPreferenceRepository, never()).findCategoryIdsByUser(any());
    }

    @Test
    //verifies cycles anchored to clearedAt timestamp once history exists
    public void testCyclesAnchoredToClearedAt() {
        UserSettings settings = new UserSettings();
        //cleared 10 days ago: cycle 1 runs from clearedAt + 7 days to clearedAt + 14 days
        LocalDateTime clearedAt = LocalDateTime.now(fixedClock).minusDays(10);
        settings.setHistoryClearedAt(clearedAt);
        testUserSettingsService.setUserSettings(settings);

        LocalDateTime expectedWindowStart = clearedAt;
        LocalDateTime expectedWindowEnd = clearedAt.plusDays(7);

        Object[] row = new Object[]{gamingCategory.getId(), 5L, clearedAt.plusDays(2)};
        when(watchHistoryRepository.findTopCategoryDataByUserInWindow(
                eq(testUser), eq(expectedWindowStart), eq(expectedWindowEnd), any(PageRequest.class)))
                .thenReturn(List.<Object[]>of(row));

        when(videoRepository.findEligibleVideosByCategories(
                eq(List.of(gamingCategory.getId())), eq(testUser.getUserId()), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(video1, video2));

        List<VideoResponseDTO> results = watchHistoryService.getRecommendations(testUser, 3);

        assertEquals(2, results.size());
        assertEquals(video1.getVideoId(), results.get(0).getVideoId());
    }

    @Test
    //verifies cycle anchored to account creation when history was never cleared
    public void testAnchoredToAccountCreationIfNeverCleared() {
        testUserSettingsService.setUserSettings(null);

        //account created 20 days ago: cycle 2 (14 to 21 days), window is days 7 to 14
        LocalDateTime createdAt = testUser.getCreatedAt();
        LocalDateTime expectedWindowStart = createdAt.plusDays(7);
        LocalDateTime expectedWindowEnd = createdAt.plusDays(14);

        Object[] row = new Object[]{musicCategory.getId(), 4L, createdAt.plusDays(10)};
        when(watchHistoryRepository.findTopCategoryDataByUserInWindow(
                eq(testUser), eq(expectedWindowStart), eq(expectedWindowEnd), any(PageRequest.class)))
                .thenReturn(List.<Object[]>of(row));

        when(videoRepository.findEligibleVideosByCategories(
                eq(List.of(musicCategory.getId())), eq(testUser.getUserId()), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(video1));

        List<VideoResponseDTO> results = watchHistoryService.getRecommendations(testUser, 3);

        assertEquals(1, results.size());
    }

    @Test
    //verifies first cycle (under 7 days old) uses picked categories
    public void testFirstCycleUsesPickedCategories() {
        testUser.setCreatedAt(LocalDateTime.now(fixedClock).minusDays(3));
        testUserSettingsService.setUserSettings(null);

        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser)))
                .thenReturn(List.of(gamingCategory.getId()));
        when(videoRepository.findEligibleVideosByCategories(
                eq(List.of(gamingCategory.getId())), eq(testUser.getUserId()), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(video1, video2));

        List<VideoResponseDTO> results = watchHistoryService.getRecommendations(testUser, 3);

        assertEquals(2, results.size());
        verify(watchHistoryRepository, never()).findTopCategoryDataByUserInWindow(any(), any(), any(), any());
    }

    @Test
    //verifies later cycle with no history falls back to picked categories if never cleared
    public void testLaterCycleWithNoHistoryFallsBackToPickedCategoriesIfNeverCleared() {
        testUserSettingsService.setUserSettings(null);

        //no history in window
        when(watchHistoryRepository.findTopCategoryDataByUserInWindow(eq(testUser), any(), any(), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser)))
                .thenReturn(List.of(gamingCategory.getId()));
        when(videoRepository.findEligibleVideosByCategories(
                eq(List.of(gamingCategory.getId())), eq(testUser.getUserId()), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(video1));

        List<VideoResponseDTO> results = watchHistoryService.getRecommendations(testUser, 3);

        assertEquals(1, results.size());
        assertEquals(video1.getVideoId(), results.get(0).getVideoId());
    }

    @Test
    //verifies minimum 10 views threshold passed to repository query
    public void testMinTenViewsEnforced() {
        testUser.setCreatedAt(LocalDateTime.now(fixedClock).minusDays(2));
        testUserSettingsService.setUserSettings(null);

        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser)))
                .thenReturn(List.of(gamingCategory.getId()));
        when(videoRepository.findEligibleVideosByCategories(
                eq(List.of(gamingCategory.getId())), eq(testUser.getUserId()), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(video1));

        List<VideoResponseDTO> results = watchHistoryService.getRecommendations(testUser, 3);

        assertEquals(1, results.size());
        verify(videoRepository).findEligibleVideosByCategories(any(), any(), eq(10), any());
    }

    @Test
    //verifies user own uploads and non-public videos excluded via repository query parameters
    public void testOwnUploadsAndNonPublicExcluded() {
        testUser.setCreatedAt(LocalDateTime.now(fixedClock).minusDays(2));
        testUserSettingsService.setUserSettings(null);

        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser)))
                .thenReturn(List.of(gamingCategory.getId()));
        when(videoRepository.findEligibleVideosByCategories(
                eq(List.of(gamingCategory.getId())), eq(testUser.getUserId()), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(video1));

        watchHistoryService.getRecommendations(testUser, 3);

        //verifies excludeUserId is passed to exclude own uploads
        verify(videoRepository).findEligibleVideosByCategories(any(), eq(testUser.getUserId()), eq(10), any());
    }

    @Test
    //verifies maximum 3 recommendations enforced in backend even when more available or requested
    public void testMaxThreeVideosEnforced() {
        testUser.setCreatedAt(LocalDateTime.now(fixedClock).minusDays(2));
        testUserSettingsService.setUserSettings(null);

        when(userCategoryPreferenceRepository.findCategoryIdsByUser(eq(testUser)))
                .thenReturn(List.of(gamingCategory.getId()));
        when(videoRepository.findEligibleVideosByCategories(
                eq(List.of(gamingCategory.getId())), eq(testUser.getUserId()), eq(10), eq(PageRequest.of(0, 3))))
                .thenReturn(List.of(video1, video2, video3, video4));

        List<VideoResponseDTO> results = watchHistoryService.getRecommendations(testUser, 10);

        assertEquals(3, results.size());
    }

    @Test
    //verifies recommendation read path does not create or persist UserSettings row
    public void testNoUserSettingsRowCreatedOnRead() {
        testUserSettingsService.setUserSettings(null);
        when(watchHistoryRepository.findTopCategoryDataByUserInWindow(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(userCategoryPreferenceRepository.findCategoryIdsByUser(any()))
                .thenReturn(Collections.emptyList());

        watchHistoryService.getRecommendations(testUser, 3);

        assertEquals(1, testUserSettingsService.findByUserIdCallCount);
        assertEquals(0, testUserSettingsService.getOrCreateCallCount);
        assertFalse(testUserSettingsService.recordHistoryClearedCalled);
    }

    @Test
    //verifies clearUserHistory updates timestamp lazily with lock
    public void testClearUserHistoryUpdatesTimestampWithLock() {
        watchHistoryService.clearUserHistory(testUser);

        verify(userRepository).findByIdWithLock(testUser.getUserId());
        verify(watchHistoryRepository).deleteByUser(testUser);
        assertTrue(testUserSettingsService.recordHistoryClearedCalled);
    }
}
