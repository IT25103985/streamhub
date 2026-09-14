package com.streamhub.social.repository;

import com.streamhub.common.entity.User;
import com.streamhub.social.entity.Dislike;
import com.streamhub.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
//repository tracking user dislike records on videos
public interface DislikeRepository extends JpaRepository<Dislike, UUID> {
    Optional<Dislike> findByVideoAndUser(Video video, User user);
    int countByVideo(Video video);
    boolean existsByVideoAndUser(Video video, User user);
}
