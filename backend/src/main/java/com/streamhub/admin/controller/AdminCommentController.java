package com.streamhub.admin.controller;

import com.streamhub.social.dto.CommentDTO;
import com.streamhub.social.service.SocialService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/comments")
//handles admin moderation endpoints for user comments
public class AdminCommentController {

    private final SocialService socialService;

    public AdminCommentController(SocialService socialService) {
        this.socialService = socialService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CONTENT_MODERATOR')")
    //lists all comments across the platform for admin review
    public ResponseEntity<List<CommentDTO>> getAllComments() {
        return ResponseEntity.ok(socialService.getAllCommentsForAdmin());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CONTENT_MODERATOR')")
    public ResponseEntity<Void> deleteCommentAsModerator(@PathVariable UUID id) {
        socialService.moderateDeleteComment(id);
        return ResponseEntity.noContent().build();
    }
}
