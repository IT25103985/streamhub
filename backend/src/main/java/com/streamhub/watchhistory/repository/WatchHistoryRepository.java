package com.streamhub.watchhistory.repository;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import com.streamhub.watchhistory.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

//repository for querying and aggregating user playback history
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, UUID> {

    Page<WatchHistory> findByUserOrderByUpdatedAtDesc(User user, Pageable pageable);

    Page<WatchHistory> findByUserOrderByWatchedAtDesc(User user, Pageable pageable);

    //latest row for a user across all videos
    Optional<WatchHistory> findFirstByUserOrderByUpdatedAtDesc(User user);

    //latest row for a user and specific video (resume playback)
    Optional<WatchHistory> findFirstByUserAndVideoOrderByUpdatedAtDesc(User user, Video video);

    //latest row by user id and video id (resume playback shortcut)
    @Query("SELECT wh FROM WatchHistory wh WHERE wh.user.userId = :userId AND wh.video.videoId = :videoId ORDER BY wh.updatedAt DESC")
    List<WatchHistory> findLatestByUserIdAndVideoId(@Param("userId") UUID userId, @Param("videoId") UUID videoId, Pageable pageable);

    void deleteByUser(User user);

    @Query("SELECT wh.video.category.id, COUNT(wh) as count " +
           "FROM WatchHistory wh " +
           "WHERE wh.user = :user AND wh.video.category IS NOT NULL " +
           "GROUP BY wh.video.category.id " +
           "ORDER BY COUNT(wh) DESC")
    List<Object[]> findMostWatchedCategoryDataByUser(@Param("user") User user, Pageable pageable);

    //retrieves top categories in window with deterministic tie break on recency and category id
    @Query("SELECT wh.video.category.id, COUNT(wh) as cnt, MAX(wh.updatedAt) as maxUpdated " +
           "FROM WatchHistory wh " +
           "WHERE wh.user = :user AND wh.video.category IS NOT NULL " +
           "  AND wh.updatedAt >= :start AND wh.updatedAt < :end " +
           "GROUP BY wh.video.category.id " +
           "ORDER BY COUNT(wh) DESC, MAX(wh.updatedAt) DESC, wh.video.category.id ASC")
    List<Object[]> findTopCategoryDataByUserInWindow(@Param("user") User user,
                                                    @Param("start") LocalDateTime start,
                                                    @Param("end") LocalDateTime end,
                                                    Pageable pageable);
    
    //top watched videos by view count for admin analytics
    @Query("SELECT wh.video.id " +
           "FROM WatchHistory wh " +
           "GROUP BY wh.video.id " +
           "ORDER BY COUNT(wh) DESC")
    List<UUID> findTopVideoIdsByWatchCount(Pageable pageable);

    //count distinct videos watched by user to determine behavioral threshold
    @Query("SELECT COUNT(DISTINCT wh.video.id) FROM WatchHistory wh WHERE wh.user = :user")
    long countDistinctWatchedVideosByUser(@Param("user") User user);
}
