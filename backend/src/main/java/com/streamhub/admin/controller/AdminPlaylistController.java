package com.streamhub.admin.controller;

import com.streamhub.playlist.dto.PlaylistResponseDTO;
import com.streamhub.playlist.service.PlaylistService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/playlists")
//admin endpoints to monitor and moderate all user playlists
public class AdminPlaylistController {

    private final PlaylistService playlistService;

    public AdminPlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CONTENT_MODERATOR')")
    //fetches complete playlist list including private ones for admin
    public ResponseEntity<List<PlaylistResponseDTO>> getAllPlaylists() {
        return ResponseEntity.ok(playlistService.getAllPlaylistsForAdmin());
    }

    @PatchMapping("/{id}/hide")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CONTENT_MODERATOR')")
    public ResponseEntity<Void> togglePlaylistHidden(@PathVariable UUID id) {
        playlistService.togglePlaylistHidden(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'CONTENT_MODERATOR')")
    public ResponseEntity<Void> deletePlaylist(@PathVariable UUID id) {
        playlistService.deletePlaylistAsModerator(id);
        return ResponseEntity.noContent().build();
    }
}
