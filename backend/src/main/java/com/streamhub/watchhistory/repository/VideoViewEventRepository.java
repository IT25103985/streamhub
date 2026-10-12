package com.streamhub.watchhistory.repository;

import com.streamhub.video.entity.Video;
import com.streamhub.watchhistory.entity.VideoViewEvent;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

//repository for logging and aggregating rolling window video view events
public interface VideoViewEventRepository extends JpaRepository<VideoViewEvent, UUID> {

    //aggregates public non-hidden videos with recent watch events meeting threshold
    @Query("SELECT e.video FROM VideoViewEvent e " +
           "WHERE e.viewedAt >= :since " +
           "AND (e.video.isHidden = false OR e.video.isHidden IS NULL) " +
           "AND (UPPER(e.video.privacy) = 'PUBLIC' OR e.video.privacy IS NULL) " +
           "GROUP BY e.video " +
           "HAVING COUNT(e) >= :minWatches " +
           "ORDER BY COUNT(e) DESC, e.video.createdAt ASC")
    List<Video> findTrendingVideosWithMinWatches(@Param("since") LocalDateTime since, 
                                                 @Param("minWatches") long minWatches, 
                                                 Pageable pageable);

    //aggregates public non-hidden videos with recent watch events below threshold for backfill
    @Query("SELECT e.video FROM VideoViewEvent e " +
           "WHERE e.viewedAt >= :since " +
           "AND (e.video.isHidden = false OR e.video.isHidden IS NULL) " +
           "AND (UPPER(e.video.privacy) = 'PUBLIC' OR e.video.privacy IS NULL) " +
           "GROUP BY e.video " +
           "HAVING COUNT(e) < :minWatches " +
           "ORDER BY COUNT(e) DESC, e.video.createdAt ASC")
    List<Video> findTrendingVideosBelowMinWatches(@Param("since") LocalDateTime since, 
                                                  @Param("minWatches") long minWatches, 
                                                  Pageable pageable);

    //counts recent watch events for a specific video in rolling window
    @Query("SELECT COUNT(e) FROM VideoViewEvent e WHERE e.video.videoId = :videoId AND e.viewedAt >= :since")
    long countRecentViewsByVideoId(@Param("videoId") UUID videoId, @Param("since") LocalDateTime since);

    //counts recent watch events for a specific video and user in rolling window
    @Query("SELECT COUNT(e) FROM VideoViewEvent e WHERE e.video.videoId = :videoId AND e.user = :user AND e.viewedAt >= :since")
    long countRecentViewsByVideoAndUser(@Param("videoId") UUID videoId, @Param("user") com.streamhub.common.entity.User user, @Param("since") LocalDateTime since);
}
