package com.streamhub.playlist.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.playlist.dto.CreatePlaylistDTO;
import com.streamhub.playlist.dto.PlaylistDetailDTO;
import com.streamhub.playlist.dto.PlaylistResponseDTO;
import com.streamhub.playlist.dto.UpdatePlaylistDTO;
import com.streamhub.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/playlists")
//controller for creating updating and watching user playlists
public class PlaylistController {

    private final PlaylistService playlistService;
    private final UserRepository userRepository;

    public PlaylistController(PlaylistService playlistService, UserRepository userRepository) {
        this.playlistService = playlistService;
        this.userRepository = userRepository;
    }

    //playlist creation endpoint
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    //creates a new playlist with optional custom cover art
    public ResponseEntity<PlaylistResponseDTO> createPlaylist(
            @RequestPart("data") @Valid CreatePlaylistDTO dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.status(HttpStatus.CREATED).body(playlistService.createPlaylist(dto, user, thumbnail));
    }

    //updates playlist name description privacy and thumbnail
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<PlaylistResponseDTO> updatePlaylist(
            @PathVariable UUID id,
            @RequestPart("data") @Valid UpdatePlaylistDTO dto,
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(playlistService.updatePlaylist(id, dto, user, thumbnail));
    }

    //user playlist collection query
    //retrieves playlists owned by the authenticated user
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<PlaylistResponseDTO>> getUserPlaylists(
            @RequestParam(defaultValue = "false") boolean onlyOwnVideos,
            @RequestParam(required = false) String search,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(playlistService.getUserPlaylists(user, onlyOwnVideos, search));
    }

    //retrieves all publicly accessible playlists
    @GetMapping("/public")
    public ResponseEntity<List<PlaylistResponseDTO>> getPublicPlaylists() {
        return ResponseEntity.ok(playlistService.getPublicPlaylists());
    }

    //public playlist search and category filter endpoint
    @GetMapping("/search")
    //searches public playlists by query and category filter
    public ResponseEntity<org.springframework.data.domain.Page<PlaylistResponseDTO>> searchPlaylists(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID categoryId,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(playlistService.searchPlaylists(query, categoryId, pageable));
    }

    //fetches visible playlists belonging to a specific owner
    @GetMapping
    public ResponseEntity<List<PlaylistResponseDTO>> getPlaylistsByOwner(
            @RequestParam UUID ownerId,
            @RequestParam(defaultValue = "false") boolean onlyOwnVideos,
            Principal principal) {
        User requester = null;
        if (principal != null) {
            requester = userRepository.findByUsername(principal.getName()).orElse(null);
        }
        return ResponseEntity.ok(playlistService.getPlaylistsByOwner(ownerId, onlyOwnVideos, requester));
    }

    //playlist details query endpoint
    @GetMapping("/{id}")
    //fetches playlist information and ordered list of videos
    public ResponseEntity<PlaylistDetailDTO> getPlaylistDetails(@PathVariable UUID id, Principal principal) {
        User user = null;
        if (principal != null) {
            user = userRepository.findByUsername(principal.getName()).orElse(null);
        }
        return ResponseEntity.ok(playlistService.getPlaylistDetails(id, user));
    }

    //playlist track append endpoint
    @PostMapping("/{playlistId}/videos")
    @PreAuthorize("isAuthenticated()")
    //appends a video to the end of the specified playlist
    public ResponseEntity<Void> addVideoToPlaylist(
            @PathVariable UUID playlistId, 
            @RequestBody java.util.Map<String, String> body,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        UUID videoId = UUID.fromString(body.get("videoId"));
        playlistService.addVideoToPlaylist(playlistId, videoId, user);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{playlistId}/videos/{videoId}")
    @PreAuthorize("isAuthenticated()")
    //removes a video from playlist and collapses remaining order
    public ResponseEntity<Void> removeVideoFromPlaylist(@PathVariable UUID playlistId, @PathVariable UUID videoId, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        playlistService.removeVideoFromPlaylist(playlistId, videoId, user);
        return ResponseEntity.noContent().build();
    }
    
    //playlist reorder position endpoint
    @PatchMapping("/{playlistId}/videos/reorder")
    @PreAuthorize("isAuthenticated()")
    //saves new customized drag and drop video ordering
    public ResponseEntity<Void> reorderVideos(
            @PathVariable UUID playlistId,
            @RequestBody List<UUID> orderedVideoIds,
            Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        playlistService.reorderVideos(playlistId, orderedVideoIds, user);
        return ResponseEntity.ok().build();
    }

    //deletes playlist after checking ownership
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deletePlaylist(@PathVariable UUID id, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        playlistService.deletePlaylist(id, user);
        return ResponseEntity.noContent().build();
    }
}
