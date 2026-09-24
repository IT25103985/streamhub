package com.streamhub.social.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.social.dto.CommentDTO;
import com.streamhub.social.dto.CreateCommentDTO;
import com.streamhub.social.dto.LikeStatusDTO;
import com.streamhub.social.service.SocialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
//endpoints for video reactions like and dislike toggles
public class SocialController {

    private final SocialService socialService;
    private final UserRepository userRepository;

    public SocialController(SocialService socialService, UserRepository userRepository) {
        this.socialService = socialService;
        this.userRepository = userRepository;
    }

    //comment creation and listing endpoints
    @PostMapping("/videos/{videoId}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> addComment(
            @PathVariable UUID videoId,
            @Valid @RequestBody CreateCommentDTO dto,
            Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CommentDTO createdComment = socialService.addComment(videoId, dto, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    @GetMapping("/videos/{videoId}/comments")
    public ResponseEntity<List<CommentDTO>> getCommentsByVideo(@PathVariable UUID videoId, Principal principal) {
        User user = (principal != null) ? userRepository.findByUsername(principal.getName()).orElse(null) : null;
        return ResponseEntity.ok(socialService.getCommentsByVideo(videoId, user));
    }

    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        socialService.deleteComment(commentId, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommentDTO> updateComment(
            @PathVariable UUID commentId, 
            @Valid @RequestBody com.streamhub.social.dto.UpdateCommentDTO dto, 
            Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        CommentDTO updatedComment = socialService.updateComment(commentId, dto, user);
        return ResponseEntity.ok(updatedComment);
    }

    //video like and dislike reaction endpoints
    @PostMapping("/videos/{videoId}/like")
    @PreAuthorize("isAuthenticated()")
    //toggles like state and removes existing dislike
    public ResponseEntity<LikeStatusDTO> toggleLike(@PathVariable UUID videoId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LikeStatusDTO status = socialService.toggleLike(videoId, user);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/videos/{videoId}/dislike")
    @PreAuthorize("isAuthenticated()")
    //toggles dislike state and removes existing like
    public ResponseEntity<LikeStatusDTO> toggleDislike(@PathVariable UUID videoId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LikeStatusDTO status = socialService.toggleDislike(videoId, user);
        return ResponseEntity.ok(status);
    }

    @GetMapping("/videos/{videoId}/like-status")
    //retrieves current like count and user reaction status
    public ResponseEntity<LikeStatusDTO> getLikeStatus(@PathVariable UUID videoId, Principal principal) {
        User user = null;
        if (principal != null) {
            user = userRepository.findByUsername(principal.getName()).orElse(null);
        }
        
        LikeStatusDTO status = socialService.getLikeStatus(videoId, user);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/comments/{commentId}/like")
    @PreAuthorize("isAuthenticated()")
    //toggles like state for a comment
    public ResponseEntity<LikeStatusDTO> toggleCommentLike(@PathVariable UUID commentId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        LikeStatusDTO status = socialService.toggleCommentLike(commentId, user);
        return ResponseEntity.ok(status);
    }
}
