package com.streamhub.follow.service;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.follow.dto.FollowCountsResponse;
import com.streamhub.follow.dto.FollowUserDTO;
import com.streamhub.follow.entity.Follow;
import com.streamhub.follow.repository.FollowRepository;
import com.streamhub.notification.entity.Notification;
import com.streamhub.notification.repository.NotificationRepository;
import com.streamhub.notification.service.NotificationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class FollowServiceTest {

    private FollowService followService;
    private NotificationService notificationService;
    private SimpMessagingTemplate messagingTemplate;

    private Map<UUID, User> userDb;
    private List<Follow> followDb;
    private List<Notification> notificationDb;
    private List<String> sentDestinations;

    private User follower;
    private User following;
    private UUID followerId;
    private UUID followingId;

    @BeforeEach
    void setUp() {
        userDb = new HashMap<>();
        followDb = new ArrayList<>();
        notificationDb = new ArrayList<>();
        sentDestinations = new ArrayList<>();

        followerId = UUID.randomUUID();
        followingId = UUID.randomUUID();

        follower = new User();
        follower.setUserId(followerId);
        follower.setUsername("alice");
        follower.setFirstName("Alice");
        follower.setLastName("Smith");

        following = new User();
        following.setUserId(followingId);
        following.setUsername("bob");
        following.setFirstName("Bob");
        following.setLastName("Jones");

        userDb.put(followerId, follower);
        userDb.put(followingId, following);

        // Dynamic proxy for UserRepository
        UserRepository userRepository = (UserRepository) Proxy.newProxyInstance(
                UserRepository.class.getClassLoader(),
                new Class<?>[]{UserRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) {
                        return Optional.ofNullable(userDb.get(args[0]));
                    }
                    if ("findByUsername".equals(name)) {
                        return userDb.values().stream().filter(u -> u.getUsername().equals(args[0])).findFirst();
                    }
                    return null;
                }
        );

        // Dynamic proxy for FollowRepository
        FollowRepository followRepository = (FollowRepository) Proxy.newProxyInstance(
                FollowRepository.class.getClassLoader(),
                new Class<?>[]{FollowRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("existsByFollower_UserIdAndFollowing_UserId".equals(name)) {
                        UUID fId = (UUID) args[0];
                        UUID fgId = (UUID) args[1];
                        return followDb.stream().anyMatch(f -> f.getFollower().getUserId().equals(fId) && f.getFollowing().getUserId().equals(fgId));
                    }
                    if ("deleteByFollower_UserIdAndFollowing_UserId".equals(name)) {
                        UUID fId = (UUID) args[0];
                        UUID fgId = (UUID) args[1];
                        followDb.removeIf(f -> f.getFollower().getUserId().equals(fId) && f.getFollowing().getUserId().equals(fgId));
                        return null;
                    }
                    if ("countByFollower_UserId".equals(name)) {
                        UUID fId = (UUID) args[0];
                        return followDb.stream().filter(f -> f.getFollower().getUserId().equals(fId)).count();
                    }
                    if ("countByFollowing_UserId".equals(name)) {
                        UUID fgId = (UUID) args[0];
                        return followDb.stream().filter(f -> f.getFollowing().getUserId().equals(fgId)).count();
                    }
                    if ("findByFollowing_UserId".equals(name)) {
                        UUID fgId = (UUID) args[0];
                        Pageable p = (Pageable) args[1];
                        List<Follow> matches = followDb.stream().filter(f -> f.getFollowing().getUserId().equals(fgId)).toList();
                        return new PageImpl<>(matches, p, matches.size());
                    }
                    if ("findByFollower_UserId".equals(name)) {
                        UUID fId = (UUID) args[0];
                        Pageable p = (Pageable) args[1];
                        List<Follow> matches = followDb.stream().filter(f -> f.getFollower().getUserId().equals(fId)).toList();
                        return new PageImpl<>(matches, p, matches.size());
                    }
                    if ("save".equals(name)) {
                        Follow f = (Follow) args[0];
                        if (f.getId() == null) f.setId(UUID.randomUUID());
                        followDb.add(f);
                        return f;
                    }
                    return null;
                }
        );

        // Dynamic proxy for NotificationRepository
        NotificationRepository notificationRepository = (NotificationRepository) Proxy.newProxyInstance(
                NotificationRepository.class.getClassLoader(),
                new Class<?>[]{NotificationRepository.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("save".equals(name)) {
                        Notification n = (Notification) args[0];
                        if (n.getId() == null) n.setId(UUID.randomUUID());
                        notificationDb.add(n);
                        return n;
                    }
                    return null;
                }
        );

        // Dynamic proxy for EntityManager
        EntityManager entityManager = (EntityManager) Proxy.newProxyInstance(
                EntityManager.class.getClassLoader(),
                new Class<?>[]{EntityManager.class},
                (proxy, method, args) -> {
                    if ("getReference".equals(method.getName())) {
                        User u = new User();
                        u.setUserId((UUID) args[1]);
                        return u;
                    }
                    return null;
                }
        );

        // Dummy MessageChannel proxy
        MessageChannel messageChannel = (MessageChannel) Proxy.newProxyInstance(
                MessageChannel.class.getClassLoader(),
                new Class<?>[]{MessageChannel.class},
                (proxy, method, args) -> true
        );

        messagingTemplate = new SimpMessagingTemplate(messageChannel) {
            @Override
            public void convertAndSendToUser(String user, String destination, Object payload) {
                sentDestinations.add(user + ":" + destination);
            }
        };

        com.streamhub.settings.service.UserSettingsService userSettingsService = new com.streamhub.settings.service.UserSettingsService(null, null) {
            @Override
            public com.streamhub.settings.entity.UserSettings getOrCreate(java.util.UUID userId) {
                return new com.streamhub.settings.entity.UserSettings();
            }
        };
        notificationService = new NotificationService(notificationRepository, entityManager, userSettingsService, messagingTemplate);

        followService = new FollowService(followRepository, userRepository, notificationService, messagingTemplate);
    }

    @Test
    void follow_success_createsFollow_notification_andLivePushes() {
        followService.follow(followerId, followingId);

        // 1. Confirm Follow row is created
        assertEquals(1, followDb.size());
        assertEquals(followerId, followDb.get(0).getFollower().getUserId());
        assertEquals(followingId, followDb.get(0).getFollowing().getUserId());

        // 2. Confirm FOLLOW notification is created
        assertEquals(1, notificationDb.size());
        assertEquals("FOLLOW", notificationDb.get(0).getType());
        assertEquals("New Follower", notificationDb.get(0).getTitle());
        assertEquals("alice started following you", notificationDb.get(0).getMessage());

        // 3. Confirm live push sent to followed user ("bob") and count updates to both users
        assertTrue(sentDestinations.contains("bob:/queue/follow-notification"));
        assertTrue(sentDestinations.contains("alice:/queue/follow-counts"));
        assertTrue(sentDestinations.contains("bob:/queue/follow-counts"));
    }

    @Test
    void follow_self_throwsBadRequest400() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                followService.follow(followerId, followerId)
        );
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Cannot follow yourself"));
        assertEquals(0, followDb.size());
    }

    @Test
    void follow_alreadyFollowing_throwsConflict409() {
        followService.follow(followerId, followingId);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                followService.follow(followerId, followingId)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("Already following this user"));
        assertEquals(1, followDb.size());
    }

    @Test
    void follow_targetUserNotFound_throwsNotFound404() {
        UUID nonExistentId = UUID.randomUUID();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                followService.follow(followerId, nonExistentId)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void unfollow_isIdempotent_andPushesCounts() {
        followService.follow(followerId, followingId);
        assertEquals(1, followDb.size());

        sentDestinations.clear();

        // Call unfollow once (204)
        followService.unfollow(followerId, followingId);
        assertEquals(0, followDb.size());

        // Call unfollow second time (idempotent 204)
        assertDoesNotThrow(() -> followService.unfollow(followerId, followingId));
        assertEquals(0, followDb.size());

        // Confirm count pushes occurred
        assertTrue(sentDestinations.contains("alice:/queue/follow-counts"));
        assertTrue(sentDestinations.contains("bob:/queue/follow-counts"));
    }

    @Test
    void getFollowers_nonOwner_throwsForbidden403() {
        UUID strangerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                followService.getFollowers(followingId, strangerId, pageable)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getFollowers_owner_returnsPaginatedList() {
        followService.follow(followerId, followingId);
        Pageable pageable = PageRequest.of(0, 10);

        Page<FollowUserDTO> result = followService.getFollowers(followingId, followingId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(followerId, result.getContent().get(0).getUserId());
        assertEquals("alice", result.getContent().get(0).getUsername());
        assertEquals("Alice Smith", result.getContent().get(0).getDisplayName());
    }

    @Test
    void getFollowing_nonOwner_throwsForbidden403() {
        UUID strangerId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                followService.getFollowing(followerId, strangerId, pageable)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void getFollowing_owner_returnsPaginatedList() {
        followService.follow(followerId, followingId);
        Pageable pageable = PageRequest.of(0, 10);

        Page<FollowUserDTO> result = followService.getFollowing(followerId, followerId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(followingId, result.getContent().get(0).getUserId());
        assertEquals("bob", result.getContent().get(0).getUsername());
        assertEquals("Bob Jones", result.getContent().get(0).getDisplayName());
    }

    @Test
    void getCounts_returnsAccurateCountsAndIsFollowing() {
        followService.follow(followerId, followingId);

        FollowCountsResponse response = followService.getCounts(followingId, followerId);

        assertEquals(followingId, response.getUserId());
        assertEquals(1L, response.getFollowerCount());
        assertEquals(0L, response.getFollowingCount());
        assertTrue(response.getIsFollowing());

        FollowCountsResponse followerResponse = followService.getCounts(followerId, followingId);
        assertEquals(followerId, followerResponse.getUserId());
        assertEquals(0L, followerResponse.getFollowerCount());
        assertEquals(1L, followerResponse.getFollowingCount());
        assertFalse(followerResponse.getIsFollowing());
    }
}
