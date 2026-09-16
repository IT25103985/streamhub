package com.streamhub.social.controller;

import com.streamhub.social.dto.CommentDTO;
import com.streamhub.social.service.SocialService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/comments")
//endpoints for adding editing and fetching video comments
public class CommentController {

    private final SocialService socialService;

    public CommentController(SocialService socialService) {
        this.socialService = socialService;
    }

    //comment thread listing endpoint block
    @GetMapping
    //retrieves paginated comment tree for video watch panel
    public ResponseEntity<Page<CommentDTO>> getComments(
            @RequestParam("videoId") UUID videoId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        
        Page<CommentDTO> comments = socialService.getPaginatedComments(videoId, page, size);
        return ResponseEntity.ok(comments);
    }
}
