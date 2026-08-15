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

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final VideoService videoService;
    private final UserRepository userRepository;

    public VideoController(VideoService videoService, UserRepository userRepository) {
        this.videoService = videoService;
        this.userRepository = userRepository;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CONTENT_CREATOR','ADMINISTRATOR')")
    public ResponseEntity<VideoResponseDTO> uploadVideo(
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("file") MultipartFile file,
            Principal principal) {
        
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        VideoResponseDTO uploadedVideo = videoService.uploadVideo(title, description, file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(uploadedVideo);
    }

    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable UUID id) {
        Resource videoResource = videoService.loadVideoAsResource(id);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("video/mp4")) // Ideally dynamically determined
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + videoResource.getFilename() + "\"")
                .body(videoResource);
    }

    @GetMapping
    public ResponseEntity<List<VideoResponseDTO>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAllVideos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<VideoResponseDTO> getVideoDetails(@PathVariable UUID id) {
        return ResponseEntity.ok(videoService.getVideoDetails(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVideo(@PathVariable UUID id, Principal principal) {
        User currentUser = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        videoService.deleteVideo(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/moderate")
    @PreAuthorize("hasRole('CONTENT_MODERATOR')")
    public ResponseEntity<Void> moderateDeleteVideo(@PathVariable UUID id) {
        videoService.moderateDeleteVideo(id);
        return ResponseEntity.noContent().build();
    }
}
