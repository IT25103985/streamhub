package com.streamhub.social.service;

import com.streamhub.common.entity.User;
import com.streamhub.social.dto.CommentDTO;
import com.streamhub.social.dto.CreateCommentDTO;
import com.streamhub.social.dto.LikeStatusDTO;
import com.streamhub.social.entity.Comment;
import com.streamhub.social.entity.Like;
import com.streamhub.social.entity.Dislike;
import com.streamhub.social.event.CommentCreatedEvent;
import com.streamhub.social.event.CommentLikedEvent;
import com.streamhub.social.event.CommentRepliedEvent;
import com.streamhub.social.event.VideoLikedEvent;
import com.streamhub.social.repository.CommentRepository;
import com.streamhub.social.repository.LikeRepository;
import com.streamhub.social.repository.DislikeRepository;
import com.streamhub.video.entity.Video;
import com.streamhub.video.repository.VideoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
//manages comments single level reply hierarchy and like reactions
public class SocialService {

    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final DislikeRepository dislikeRepository;
    private final VideoRepository videoRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.streamhub.social.repository.CommentLikeRepository commentLikeRepository;

    @org.springframework.beans.factory.annotation.Autowired
    public SocialService(CommentRepository commentRepository, 
                         LikeRepository likeRepository, 
                         DislikeRepository dislikeRepository, 
                         VideoRepository videoRepository,
                         ApplicationEventPublisher eventPublisher,
                         com.streamhub.social.repository.CommentLikeRepository commentLikeRepository) {
        this.commentRepository = commentRepository;
        this.likeRepository = likeRepository;
        this.dislikeRepository = dislikeRepository;
        this.videoRepository = videoRepository;
        this.eventPublisher = eventPublisher;
        this.commentLikeRepository = commentLikeRepository;
    }

    public SocialService(CommentRepository commentRepository, 
                         LikeRepository likeRepository, 
                         DislikeRepository dislikeRepository, 
                         VideoRepository videoRepository) {
        this(commentRepository, likeRepository, dislikeRepository, videoRepository, null, null);
    }

    //validates user access permission for private or hidden videos
    private boolean canUserAccessVideo(Video video, User user) {
        if (video == null) return false;
        boolean isOwner = user != null && video.getOwner() != null && user.getUserId().equals(video.getOwner().getUserId());
        boolean isAdminOrMod = user != null && ("ADMINISTRATOR".equals(user.getRole()) || "CONTENT_MODERATOR".equals(user.getRole()));
        if (video.isHidden() && !isOwner && !isAdminOrMod) return false;
        if ("PRIVATE".equalsIgnoreCase(video.getPrivacy()) && !isOwner && !isAdminOrMod) return false;
        return true;
    }

    @Transactional
    //comment creation and nesting logic
    public CommentDTO addComment(UUID videoId, CreateCommentDTO dto, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));

        if (!canUserAccessVideo(video, user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        String trimmedText = dto.getText() != null ? dto.getText().trim() : "";
        Comment comment = new Comment(video, user, trimmedText);
        Comment parent = null;

        if (dto.getParentCommentId() != null) {
            parent = commentRepository.findById(dto.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            
            //flatten nested replies into single level thread
            if (parent.getParentComment() != null) {
                comment.setParentComment(parent.getParentComment());
            } else {
                comment.setParentComment(parent);
            }
        }

        Comment saved = commentRepository.save(comment);

        if (eventPublisher != null && user != null) {
            String commentPreview = trimmedText.length() > 60 ? trimmedText.substring(0, 57) + "..." : trimmedText;

            // Notify video owner if another user commented on their video
            if (video.getOwner() != null && !video.getOwner().getUserId().equals(user.getUserId())) {
                eventPublisher.publishEvent(new CommentCreatedEvent(
                        video.getVideoId(),
                        video.getTitle(),
                        user.getUserId(),
                        user.getUsername(),
                        video.getOwner().getUserId(),
                        commentPreview
                ));
            }

            // Notify parent comment author if someone replied to their comment
            if (parent != null && parent.getAuthor() != null) {
                UUID parentAuthorId = parent.getAuthor().getUserId();
                UUID videoOwnerId = video.getOwner() != null ? video.getOwner().getUserId() : null;
                if (!parentAuthorId.equals(user.getUserId()) && !parentAuthorId.equals(videoOwnerId)) {
                    eventPublisher.publishEvent(new CommentRepliedEvent(
                            video.getVideoId(),
                            video.getTitle(),
                            user.getUserId(),
                            user.getUsername(),
                            parentAuthorId,
                            commentPreview
                    ));
                }
            }
        }

        return mapToCommentDTO(saved, user);
    }

    //retrieves page of top level comments and replies for video
    @Transactional(readOnly = true)
    public Page<CommentDTO> getPaginatedComments(UUID videoId, int page, int size) {
        return getPaginatedComments(videoId, page, size, null);
    }

    @Transactional(readOnly = true)
    public Page<CommentDTO> getPaginatedComments(UUID videoId, int page, int size, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));

        if (!canUserAccessVideo(video, user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        Pageable pageable = PageRequest.of(page, size);
        return commentRepository.findVisibleByVideoAndParentCommentIsNullOrderByCreatedAtDesc(video, pageable)
                .map(comment -> mapToCommentDTO(comment, user));
    }

    //fetches all top level comments with replies for a video
    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByVideo(UUID videoId) {
        return getCommentsByVideo(videoId, null);
    }

    @Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByVideo(UUID videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));

        if (!canUserAccessVideo(video, user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        return commentRepository.findVisibleByVideoAndParentCommentIsNullOrderByCreatedAtDesc(video).stream()
                .map(comment -> mapToCommentDTO(comment, user))
                .collect(Collectors.toList());
    }

    //deletes comment after checking user ownership
    @Transactional
    public void deleteComment(UUID commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        if (!comment.getAuthor().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only delete your own comments");
        }

        deleteCommentSafely(comment);
    }

    //administratively removes comment and associated likes
    @Transactional
    public void moderateDeleteComment(UUID commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        deleteCommentSafely(comment);
    }

    // Deletes a comment and cleans up CommentLike rows first.
    // CommentLike has no cascade so we must remove likes manually before
    // deleting the comment or any of its replies, otherwise SQL throws a FK violation.
    private void deleteCommentSafely(Comment comment) {
        // Remove likes on every reply first
        if (comment.getReplies() != null) {
            for (Comment reply : comment.getReplies()) {
                commentLikeRepository.deleteAllByComment(reply);
            }
        }
        // Remove likes on the comment itself
        commentLikeRepository.deleteAllByComment(comment);
        // Now it is safe to delete — Hibernate cascades the reply rows via CascadeType.ALL
        commentRepository.delete(comment);
    }

    //retrieves all comments across platform for moderation review
    @Transactional(readOnly = true)
    public List<CommentDTO> getAllCommentsForAdmin() {
        return commentRepository.findAll().stream()
                .map(comment -> mapToCommentDTO(comment, null))
                .collect(Collectors.toList());
    }

    @Transactional
    //like reaction state management logic
    public LikeStatusDTO toggleLike(UUID videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));

        if (!canUserAccessVideo(video, user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        Optional<Like> existingLike = likeRepository.findByVideoAndUser(video, user);

        if (existingLike.isPresent()) {
            likeRepository.delete(existingLike.get());
        } else {
            //remove opposite reaction if present
            dislikeRepository.findByVideoAndUser(video, user).ifPresent(dislikeRepository::delete);
            
            Like like = new Like(video, user);
            likeRepository.save(like);

            if (eventPublisher != null && video.getOwner() != null && user != null 
                    && !video.getOwner().getUserId().equals(user.getUserId())) {
                eventPublisher.publishEvent(new VideoLikedEvent(
                        video.getVideoId(),
                        video.getTitle(),
                        user.getUserId(),
                        user.getUsername(),
                        video.getOwner().getUserId()
                ));
            }
        }

        int likeCount = likeRepository.countByVideo(video);
        boolean userLiked = !existingLike.isPresent();
        int dislikeCount = dislikeRepository.countByVideo(video);
        boolean userDisliked = false;

        return new LikeStatusDTO(likeCount, userLiked, dislikeCount, userDisliked);
    }

    @Transactional
    //dislike reaction state management logic
    public LikeStatusDTO toggleDislike(UUID videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));

        if (!canUserAccessVideo(video, user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        Optional<Dislike> existingDislike = dislikeRepository.findByVideoAndUser(video, user);

        if (existingDislike.isPresent()) {
            dislikeRepository.delete(existingDislike.get());
        } else {
            //remove opposite reaction if present
            likeRepository.findByVideoAndUser(video, user).ifPresent(likeRepository::delete);

            Dislike dislike = new Dislike(video, user);
            dislikeRepository.save(dislike);
        }

        int likeCount = likeRepository.countByVideo(video);
        boolean userLiked = false;
        int dislikeCount = dislikeRepository.countByVideo(video);
        boolean userDisliked = !existingDislike.isPresent();

        return new LikeStatusDTO(likeCount, userLiked, dislikeCount, userDisliked);
    }

    //retrieves like and dislike totals and current user state for video
    @Transactional(readOnly = true)
    public LikeStatusDTO getLikeStatus(UUID videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found"));

        if (!canUserAccessVideo(video, user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        int likeCount = likeRepository.countByVideo(video);
        int dislikeCount = dislikeRepository.countByVideo(video);
        boolean userLiked = false;
        boolean userDisliked = false;

        if (user != null) {
            userLiked = likeRepository.existsByVideoAndUser(video, user);
            userDisliked = dislikeRepository.existsByVideoAndUser(video, user);
        }

        return new LikeStatusDTO(likeCount, userLiked, dislikeCount, userDisliked);
    }

    //toggles like upvote on a comment and updates reaction count
    @Transactional
    public LikeStatusDTO toggleCommentLike(UUID commentId, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Comment not found"));

        if (!canUserAccessVideo(comment.getVideo(), user)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Video not found");
        }

        Optional<com.streamhub.social.entity.CommentLike> existingLike = commentLikeRepository.findByCommentAndUser(comment, user);

        if (existingLike.isPresent()) {
            commentLikeRepository.delete(existingLike.get());
        } else {
            com.streamhub.social.entity.CommentLike like = new com.streamhub.social.entity.CommentLike(comment, user);
            commentLikeRepository.save(like);

            if (eventPublisher != null && comment.getAuthor() != null && user != null 
                    && !comment.getAuthor().getUserId().equals(user.getUserId())) {
                eventPublisher.publishEvent(new CommentLikedEvent(
                        comment.getCommentId(),
                        comment.getVideo().getTitle(),
                        user.getUserId(),
                        user.getUsername(),
                        comment.getAuthor().getUserId()
                ));
            }
        }

        int likeCount = commentLikeRepository.countByComment(comment);
        boolean userLiked = !existingLike.isPresent();

        return new LikeStatusDTO(likeCount, userLiked, 0, false);
    }

    //records single like for a video
    @Transactional
    public void addLike(UUID videoId, User user) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));

        try {
            Like like = new Like(video, user);
            likeRepository.saveAndFlush(like);

            if (eventPublisher != null && video.getOwner() != null && user != null 
                    && !video.getOwner().getUserId().equals(user.getUserId())) {
                eventPublisher.publishEvent(new VideoLikedEvent(
                        video.getVideoId(),
                        video.getTitle(),
                        user.getUserId(),
                        user.getUsername(),
                        video.getOwner().getUserId()
                ));
            }
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("User already liked this video");
        }
    }

    //updates text content of an existing comment owned by user
    @Transactional
    public CommentDTO updateComment(UUID commentId, com.streamhub.social.dto.UpdateCommentDTO dto, User user) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        if (!comment.getAuthor().getUserId().equals(user.getUserId())) {
            throw new RuntimeException("Unauthorized: You can only edit your own comments");
        }
        
        comment.setText(dto.getText() != null ? dto.getText().trim() : "");
        Comment saved = commentRepository.save(comment);
        return mapToCommentDTO(saved, user);
    }

    private CommentDTO mapToCommentDTO(Comment comment, User user) {
        java.time.LocalDateTime createdAt = comment.getCreatedAt() != null ? comment.getCreatedAt() : java.time.LocalDateTime.now();
        UUID parentId = comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null;
        
        java.util.List<CommentDTO> repliesDTO = new java.util.ArrayList<>();
        if (comment.getParentComment() == null && comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            repliesDTO = comment.getReplies().stream()
                    .filter(r -> !r.isHidden())
                    .map(r -> mapToCommentDTO(r, user))
                    .collect(Collectors.toList());
        }

        CommentDTO dto = new CommentDTO(
                comment.getCommentId(),
                comment.getVideo().getVideoId(),
                comment.getVideo().getTitle(),
                comment.getAuthor().getUserId(),
                comment.getAuthor().getUsername(),
                comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName(),
                comment.getAuthor().getAvatarUrl(),
                comment.getText(),
                createdAt,
                parentId,
                repliesDTO,
                comment.isHidden()
        );
        
        if (commentLikeRepository != null) {
            dto.setLikeCount(commentLikeRepository.countByComment(comment));
            if (user != null) {
                dto.setLikedByMe(commentLikeRepository.existsByCommentAndUser(comment, user));
            }
        }
        
        return dto;
    }

    //counts total number of comments across platform
    @Transactional(readOnly = true)
    public long countComments() {
        return commentRepository.count();
    }

    //counts total number of video likes across platform
    @Transactional(readOnly = true)
    public long countLikes() {
        return likeRepository.count();
    }
}
