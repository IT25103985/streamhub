package com.streamhub.watchhistory.repository;

import com.streamhub.common.entity.User;
import com.streamhub.video.entity.Video;
import com.streamhub.watchhistory.entity.WatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//repository for querying and aggregating user playback history
public interface WatchHistoryRepository extends JpaRepository<WatchHistory, UUID> {

    Page<WatchHistory> findByUserOrderByUpdatedAtDesc(User user, Pageable pageable);

    Page<WatchHistory> findByUserOrderByWatchedAtDesc(User user, Pageable pageable);

    Optional<WatchHistory> findByUserAndVideo(User user, Video video);

    Optional<WatchHistory> findByUser_UserIdAndVideo_VideoId(UUID userId, UUID videoId);

    void deleteByUser(User user);

    void deleteByUserAndVideo_VideoId(User user, UUID videoId);

    @Query("SELECT wh.video.category.id, COUNT(wh) as count " +
           "FROM WatchHistory wh " +
           "WHERE wh.user = :user AND wh.video.category IS NOT NULL " +
           "GROUP BY wh.video.category.id " +
           "ORDER BY COUNT(wh) DESC")
    List<Object[]> findMostWatchedCategoryDataByUser(@Param("user") User user, Pageable pageable);
    
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
