package com.streamhub.follow.repository;

import com.streamhub.follow.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
//repository interface for persisting and querying user follow relationships
public interface FollowRepository extends JpaRepository<Follow, UUID> {

    boolean existsByFollower_UserIdAndFollowing_UserId(UUID followerId, UUID followingId);

    void deleteByFollower_UserIdAndFollowing_UserId(UUID followerId, UUID followingId);

    long countByFollower_UserId(UUID followerId);

    long countByFollowing_UserId(UUID followingId);

    Page<Follow> findByFollowing_UserId(UUID followingId, Pageable pageable);

    Page<Follow> findByFollower_UserId(UUID followerId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT new com.streamhub.follow.dto.SidebarFollowedUserDTO(" +
           "f.following.userId, f.following.username, f.following.firstName, f.following.lastName, f.following.avatarUrl, " +
           "COALESCE(SUM(CASE WHEN vve.viewedAt >= :since THEN 1 ELSE 0 END), 0L)) " +
           "FROM Follow f " +
           "LEFT JOIN Video v ON v.owner.userId = f.following.userId " +
           "LEFT JOIN VideoViewEvent vve ON vve.video.videoId = v.videoId " +
           "WHERE f.follower.userId = :followerId " +
           "GROUP BY f.following.userId, f.following.username, f.following.firstName, f.following.lastName, f.following.avatarUrl, f.createdAt " +
           "ORDER BY COALESCE(SUM(CASE WHEN vve.viewedAt >= :since THEN 1 ELSE 0 END), 0L) DESC, f.createdAt DESC")
    java.util.List<com.streamhub.follow.dto.SidebarFollowedUserDTO> findTopFollowedByRecentViews(
            @org.springframework.data.repository.query.Param("followerId") UUID followerId, 
            @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since, 
            Pageable pageable);
}
