package com.streamhub.follow.service;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.follow.dto.FollowCountsResponse;
import com.streamhub.follow.dto.FollowUserDTO;
import com.streamhub.follow.entity.Follow;
import com.streamhub.follow.repository.FollowRepository;
import com.streamhub.notification.dto.NotificationResponse;
import com.streamhub.notification.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
//service managing follow relationships notifications and live stomp websocket updates
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate messagingTemplate;

    public FollowService(FollowRepository followRepository,
                         UserRepository userRepository,
                         NotificationService notificationService,
                         SimpMessagingTemplate messagingTemplate) {
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public void follow(UUID followerId, UUID followingId) {
        if (followerId.equals(followingId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot follow yourself");
        }

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Follower user not found"));

        User following = userRepository.findById(followingId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Target user not found"));

        if (followRepository.existsByFollower_UserIdAndFollowing_UserId(followerId, followingId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already following this user");
        }

        Follow follow = new Follow(follower, following);
        followRepository.save(follow);

        // Step 4: Create notification for followed user
        NotificationResponse notification = notificationService.createNotification(
                followingId,
                "FOLLOW",
                "New Follower",
                follower.getUsername() + " started following you",
                "USER",
                followerId
        );

        // Live push notification to followed user
        if (notification != null) {
            try {
                messagingTemplate.convertAndSendToUser(
                        following.getUsername(),
                        "/queue/follow-notification",
                        notification
                );
            } catch (Exception e) {
                System.err.println("Failed to push follow notification: " + e.getMessage());
            }
        }

        // Live push updated counts to both follower and followed user
        pushCounts(follower);
        pushCounts(following);
    }

    @Transactional
    public void unfollow(UUID followerId, UUID followingId) {
        followRepository.deleteByFollower_UserIdAndFollowing_UserId(followerId, followingId);

        userRepository.findById(followerId).ifPresent(this::pushCounts);
        userRepository.findById(followingId).ifPresent(this::pushCounts);
    }

    @Transactional(readOnly = true)
    public FollowCountsResponse getCounts(UUID targetUserId, UUID requesterIdOrNull) {
        long followerCount = followRepository.countByFollowing_UserId(targetUserId);
        long followingCount = followRepository.countByFollower_UserId(targetUserId);
        Boolean isFollowing = (requesterIdOrNull != null)
                && followRepository.existsByFollower_UserIdAndFollowing_UserId(requesterIdOrNull, targetUserId);

        return new FollowCountsResponse(targetUserId, followerCount, followingCount, isFollowing);
    }

    @Transactional(readOnly = true)
    public Page<FollowUserDTO> getFollowers(UUID ownerId, UUID requesterId, Pageable pageable) {
        if (requesterId == null || !requesterId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Only owner can view follower list");
        }

        return followRepository.findByFollowing_UserId(ownerId, pageable)
                .map(f -> new FollowUserDTO(
                        f.getFollower().getUserId(),
                        f.getFollower().getUsername(),
                        f.getFollower().getFirstName(),
                        f.getFollower().getLastName(),
                        f.getFollower().getAvatarUrl()
                ));
    }

    @Transactional(readOnly = true)
    public Page<FollowUserDTO> getFollowing(UUID ownerId, UUID requesterId, Pageable pageable) {
        if (requesterId == null || !requesterId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Forbidden: Only owner can view following list");
        }

        return followRepository.findByFollower_UserId(ownerId, pageable)
                .map(f -> new FollowUserDTO(
                        f.getFollowing().getUserId(),
                        f.getFollowing().getUsername(),
                        f.getFollowing().getFirstName(),
                        f.getFollowing().getLastName(),
                        f.getFollowing().getAvatarUrl()
                ));
    }

    @Transactional(readOnly = true)
    public com.streamhub.follow.dto.SidebarFollowResponse getSidebarFollowing(UUID requesterId) {
        long totalFollowing = followRepository.countByFollower_UserId(requesterId);
        
        if (totalFollowing == 0) {
            return new com.streamhub.follow.dto.SidebarFollowResponse(0, java.util.Collections.emptyList());
        }

        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(7);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 50);
        java.util.List<com.streamhub.follow.dto.SidebarFollowedUserDTO> topFollowed = 
                followRepository.findTopFollowedByRecentViews(requesterId, sevenDaysAgo, pageable);

        return new com.streamhub.follow.dto.SidebarFollowResponse(totalFollowing, topFollowed);
    }

    private void pushCounts(User user) {
        try {
            long followerCount = followRepository.countByFollowing_UserId(user.getUserId());
            long followingCount = followRepository.countByFollower_UserId(user.getUserId());
            FollowCountsResponse counts = new FollowCountsResponse(user.getUserId(), followerCount, followingCount, null);

            messagingTemplate.convertAndSendToUser(
                    user.getUsername(),
                    "/queue/follow-counts",
                    counts
            );
        } catch (Exception e) {
            System.err.println("Failed to push follow counts to " + user.getUsername() + ": " + e.getMessage());
        }
    }
}
