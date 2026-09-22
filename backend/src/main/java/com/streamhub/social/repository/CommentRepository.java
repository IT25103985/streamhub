package com.streamhub.social.repository;

import com.streamhub.social.entity.Comment;
import com.streamhub.video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
//repository querying comments by video id with nesting support
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByVideoAndParentCommentIsNullOrderByCreatedAtDesc(Video video);
    Page<Comment> findByVideoAndParentCommentIsNullOrderByCreatedAtDesc(Video video, Pageable pageable);

    @Query("SELECT c FROM Comment c WHERE c.video = :video AND c.parentComment IS NULL AND (c.isHidden = false OR c.isHidden IS NULL) ORDER BY c.createdAt DESC")
    List<Comment> findVisibleByVideoAndParentCommentIsNullOrderByCreatedAtDesc(@Param("video") Video video);

    @Query("SELECT c FROM Comment c WHERE c.video = :video AND c.parentComment IS NULL AND (c.isHidden = false OR c.isHidden IS NULL) ORDER BY c.createdAt DESC")
    Page<Comment> findVisibleByVideoAndParentCommentIsNullOrderByCreatedAtDesc(@Param("video") Video video, Pageable pageable);
}

