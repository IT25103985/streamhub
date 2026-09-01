package com.streamhub.preferences.controller;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.common.entity.User;
import com.streamhub.preferences.dto.CategoryPreferencesRequestDTO;
import com.streamhub.preferences.service.UserCategoryPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/users/me", "/api/users/me"})
//controller managing user onboarding interests status and preference submission
public class UserCategoryPreferenceController {

    private final UserCategoryPreferenceService userCategoryPreferenceService;
    private final UserRepository userRepository;

    public UserCategoryPreferenceController(UserCategoryPreferenceService userCategoryPreferenceService,
                                            UserRepository userRepository) {
        this.userCategoryPreferenceService = userCategoryPreferenceService;
        this.userRepository = userRepository;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByUsername(principal.getName()).orElse(null);
    }

    //checks whether authenticated user needs to complete onboarding
    @GetMapping("/needs-onboarding")
    @PreAuthorize("isAuthenticated()")
    //returns boolean flag indicating if category interests onboarding is required
    public ResponseEntity<Map<String, Boolean>> checkNeedsOnboarding(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        boolean needsOnboarding = userCategoryPreferenceService.needsOnboarding(user);
        return ResponseEntity.ok(Map.of("needsOnboarding", needsOnboarding));
    }

    //submits selected onboarding category preferences
    @PostMapping("/category-preferences")
    @PreAuthorize("isAuthenticated()")
    //saves at least 3 preferred categories and marks onboarding complete
    public ResponseEntity<Map<String, Object>> saveCategoryPreferences(
            Principal principal,
            @RequestBody CategoryPreferencesRequestDTO request) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        userCategoryPreferenceService.saveCategoryPreferences(user, request != null ? request.getCategoryIds() : null);
        return ResponseEntity.ok(Map.of("success", true, "message", "Category preferences saved successfully"));
    }

    //dismisses/skips onboarding and marks hasSetInterests true
    @PostMapping("/skip-onboarding")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> skipOnboarding(Principal principal) {
        User user = getAuthenticatedUser(principal);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userCategoryPreferenceService.skipOnboarding(user);
        return ResponseEntity.ok(Map.of("success", true, "message", "Onboarding skipped successfully"));
    }
}
