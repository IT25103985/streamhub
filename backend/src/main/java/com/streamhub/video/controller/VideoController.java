package com.streamhub.video.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.video.dto.VideoResponseDTO;
import com.streamhub.video.service.VideoService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.support.ResourceRegion;

import java.io.IOException;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
//handles video upload streaming searching and details retrieval
public class VideoController {

    private final VideoService videoService;
    private final UserRepository userRepository;
    private final com.streamhub.watchhistory.service.WatchHistoryService watchHistoryService;

    public VideoController(VideoService videoService, UserRepository userRepository, com.streamhub.watchhistory.service.WatchHistoryService watchHistoryService) {
        this.videoService = videoService;
        this.userRepository = userRepository;
        this.watchHistoryService = watchHistoryService;
    }

    @PostMapping("/{id}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable UUID id, Principal principal) {
        User currentUser = principal != null ? userRepository.findByUsername(principal.getName()).orElse(null) : null;
        videoService.incrementViewCount(id, currentUser);
        return ResponseEntity.ok().build();
    }

    //public trending videos endpoint
    @GetMapping("/trending")
    //retrieves platform trending videos meeting engagement threshold
    public ResponseEntity<List<VideoResponseDTO>> getTrendingVideos(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(watchHistoryService.getPlatformTrendingVideos(limit));
    }

    @GetMapping("/{id}/resume-position")
    public ResponseEntity<Map<String, Integer>> getResumePosition(@PathVariable UUID id, Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("positionSeconds", 0));
        }
        User user = userRepository.findByUsername(principal.getName()).orElse(null);
        int position = watchHistoryService.getResumePosition(user, id);
        return ResponseEntity.ok(Map.of("positionSeconds", position));
    }

    //video upload endpoint block
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    //handles multipart upload of mp4 video and thumbnail
    public ResponseEntity<VideoResponseDTO> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("categoryId") UUID categoryId,
            @RequestParam(value = "privacy", required = false, defaultValue = "PUBLIC") String privacy,
            @RequestParam(value = "durationSeconds", required = false) Integer durationSeconds,
            @RequestParam("file") MultipartFile file,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        VideoResponseDTO uploadedVideo = videoService.uploadVideo(title, description, categoryId, file, thumbnail, user, privacy, durationSeconds);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedVideo);
    }

    @PatchMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VideoResponseDTO> replaceThumbnail(
            @PathVariable UUID id,
            @RequestParam("thumbnail") MultipartFile thumbnail,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        VideoResponseDTO updatedVideo = videoService.replaceThumbnail(id, thumbnail, user);
        return ResponseEntity.ok(updatedVideo);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<VideoResponseDTO> updateVideo(
            @PathVariable UUID id,
            @RequestBody com.streamhub.video.dto.VideoUpdateDTO updateDTO,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        VideoResponseDTO updatedVideo = videoService.updateVideo(id, updateDTO.getTitle(), updateDTO.getDescription(), updateDTO.getCategoryId(), updateDTO.getPrivacy(), user);
        return ResponseEntity.ok(updatedVideo);
    }

    //video streaming byte range endpoint
    @GetMapping("/{id}/stream")
    //supports range requests for smooth byte chunk video streaming
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable UUID id,
            @RequestHeader HttpHeaders headers,
            Principal principal) throws IOException {
        
        User currentUser = principal != null ? userRepository.findByUsername(principal.getName()).orElse(null) : null;
        Resource videoResource = videoService.loadVideoAsResource(id, currentUser);
        
        long contentLength = videoResource.contentLength();
        ResourceRegion region;
        
        HttpRange range = headers.getRange().isEmpty() ? null : headers.getRange().get(0);
        
        if (range != null) {
            long start = range.getRangeStart(contentLength);
            long end = range.getRangeEnd(contentLength);
            long rangeLength = Math.min(1024 * 1024, end - start + 1); //chunk video stream in 1mb blocks
            region = new ResourceRegion(videoResource, start, rangeLength);
            
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .body(region);
        } else {
            long rangeLength = Math.min(1024 * 1024, contentLength); //chunk video stream in 1mb blocks
            region = new ResourceRegion(videoResource, 0, rangeLength);
            
            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .body(region);
        }
    }

    //video library search and filter endpoint
    @GetMapping
    //searches and filters videos by category uploader or title
    public ResponseEntity<org.springframework.data.domain.Page<VideoResponseDTO>> getAllVideos(
            @RequestParam(required = false) String uploaderId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID categoryId,
            org.springframework.data.domain.Pageable pageable,
            Principal principal) {
        
        User currentUser = principal != null ? userRepository.findByUsername(principal.getName()).orElse(null) : null;

        UUID parsedUploaderId = null;
        if ("me".equalsIgnoreCase(uploaderId) && currentUser != null) {
            parsedUploaderId = currentUser.getUserId();
        } else if (uploaderId != null) {
            try {
                parsedUploaderId = UUID.fromString(uploaderId);
            } catch (IllegalArgumentException e) {
                //ignore malformed uuid and proceed
            }
        }
        
        return ResponseEntity.ok(videoService.getAllVideos(parsedUploaderId, search, categoryId, pageable, currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponseDTO> getVideoDetails(@PathVariable UUID id, Principal principal) {
        User currentUser = principal != null ? userRepository.findByUsername(principal.getName()).orElse(null) : null;
        return ResponseEntity.ok(videoService.getVideoDetails(id, currentUser));
    }

    @PatchMapping("/{id}/toggle-hide")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CONTENT_MODERATOR')")
    public ResponseEntity<VideoResponseDTO> toggleVideoHidden(@PathVariable UUID id) {
        return ResponseEntity.ok(videoService.toggleVideoHidden(id));
    }

    //video deletion endpoint
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id, Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        videoService.deleteVideo(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/moderate")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CONTENT_MODERATOR')")
    public ResponseEntity<Void> moderateDeleteVideo(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            @RequestBody(required = false) Map<String, String> body,
            Principal principal) {
        String effectiveReason = (body != null && body.containsKey("reason")) ? body.get("reason") : reason;
        User moderator = principal != null ? userRepository.findByUsername(principal.getName()).orElse(null) : null;
        videoService.moderateDeleteVideo(id, effectiveReason, moderator);
        return ResponseEntity.noContent().build();
    }
}
