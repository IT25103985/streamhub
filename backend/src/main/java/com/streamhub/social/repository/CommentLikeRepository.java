package com.streamhub.social.repository;

import com.streamhub.common.entity.User;
import com.streamhub.social.entity.Comment;
import com.streamhub.social.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, UUID> {
    Optional<CommentLike> findByCommentAndUser(Comment comment, User user);
    int countByComment(Comment comment);
    boolean existsByCommentAndUser(Comment comment, User user);
}
