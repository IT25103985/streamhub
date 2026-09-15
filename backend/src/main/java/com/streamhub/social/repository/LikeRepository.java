package com.streamhub.social.repository;

import com.streamhub.common.entity.User;
import com.streamhub.social.entity.Like;
import com.streamhub.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
//repository tracking user like records on videos
public interface LikeRepository extends JpaRepository<Like, UUID> {
    Optional<Like> findByVideoAndUser(Video video, User user);
    int countByVideo(Video video);
    boolean existsByVideoAndUser(Video video, User user);
}
