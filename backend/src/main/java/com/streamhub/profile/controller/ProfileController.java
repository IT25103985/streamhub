package com.streamhub.profile.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.profile.dto.ProfileResponseDTO;
import com.streamhub.profile.dto.ProfileUpdateDTO;
import com.streamhub.profile.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/users")
//controller for managing user profiles avatars and cover images
public class ProfileController {

    private final ProfileService profileService;
    private final UserRepository userRepository;

    public ProfileController(ProfileService profileService, UserRepository userRepository) {
        this.profileService = profileService;
        this.userRepository = userRepository;
    }

    //profile details and update endpoints
    @GetMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    //loads full profile details of logged in user
    public ResponseEntity<ProfileResponseDTO> getMyProfile(Principal principal) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(profileService.getMyProfile(user));
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<ProfileResponseDTO> getPublicProfile(@PathVariable java.util.UUID userId) {
        return ResponseEntity.ok(profileService.getPublicProfile(userId));
    }

    @PatchMapping("/me/profile")
    @PreAuthorize("isAuthenticated()")
    //updates first name last name and bio
    public ResponseEntity<ProfileResponseDTO> updateProfile(@Valid @RequestBody ProfileUpdateDTO dto, Principal principal) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(profileService.updateProfile(user, dto));
    }

    //avatar and cover upload endpoints
    @PostMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    //uploads and updates avatar image file
    public ResponseEntity<ProfileResponseDTO> uploadAvatar(@RequestParam("file") MultipartFile file, Principal principal) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(profileService.uploadAvatar(user, file));
    }

    @PostMapping(value = "/me/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    //uploads and updates profile header banner image
    public ResponseEntity<ProfileResponseDTO> uploadCover(@RequestParam("file") MultipartFile file, Principal principal) {
        User user = getCurrentUser(principal);
        return ResponseEntity.ok(profileService.uploadCover(user, file));
    }

    //static media serving endpoints
    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        Resource resource = profileService.loadAvatarAsResource(filename);
        String contentType = determineContentType(filename);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/cover/{filename:.+}")
    public ResponseEntity<Resource> getCover(@PathVariable String filename) {
        Resource resource = profileService.loadCoverAsResource(filename);
        String contentType = determineContentType(filename);
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    private User getCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    
    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG_VALUE;
        if (lower.endsWith(".webp")) return "image/webp";
        return MediaType.IMAGE_JPEG_VALUE; //default fallback image format
    }
}
