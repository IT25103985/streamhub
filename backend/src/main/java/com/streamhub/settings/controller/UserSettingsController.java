package com.streamhub.settings.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.settings.dto.UserSettingsDTO;
import com.streamhub.settings.entity.UserSettings;
import com.streamhub.settings.service.UserSettingsService;
import com.streamhub.watchhistory.service.WatchHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/settings")
@PreAuthorize("isAuthenticated()")
//controller managing personal preferences playback settings and account deletion
public class UserSettingsController {

    private final UserSettingsService userSettingsService;
    private final WatchHistoryService watchHistoryService;
    private final UserRepository userRepository;

    public UserSettingsController(UserSettingsService userSettingsService, WatchHistoryService watchHistoryService, UserRepository userRepository) {
        this.userSettingsService = userSettingsService;
        this.watchHistoryService = watchHistoryService;
        this.userRepository = userRepository;
    }

    //retrieves authenticated user entity from security principal
    private User getCurrentUser(Principal principal) {
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    //retrieves or initializes user settings profile
    @GetMapping
    public ResponseEntity<UserSettingsDTO> getSettings(Principal principal) {
        User user = getCurrentUser(principal);
        UserSettings settings = userSettingsService.getOrCreate(user.getUserId());
        return ResponseEntity.ok(new UserSettingsDTO(settings));
    }

    //updates user notification and playback settings
    @PutMapping
    public ResponseEntity<UserSettingsDTO> updateSettings(Principal principal, @RequestBody Map<String, Boolean> updates) {
        User user = getCurrentUser(principal);
        UserSettings settings = userSettingsService.update(user.getUserId(), updates);
        return ResponseEntity.ok(new UserSettingsDTO(settings));
    }

    //clears watch history records for the authenticated user
    @PostMapping("/watch-history/clear")
    public ResponseEntity<Void> clearWatchHistory(Principal principal) {
        User user = getCurrentUser(principal);
        watchHistoryService.clearUserHistory(user);
        return ResponseEntity.noContent().build();
    }

    //permanently deletes account and associated user data
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(Principal principal) {
        User user = getCurrentUser(principal);
        userSettingsService.deleteAccount(user.getUserId());
        return ResponseEntity.noContent().build();
    }
}

