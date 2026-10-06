package com.streamhub.preferences;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.category.entity.Category;
import com.streamhub.category.repository.CategoryRepository;
import com.streamhub.common.entity.User;
import com.streamhub.preferences.controller.UserCategoryPreferenceController;
import com.streamhub.preferences.dto.CategoryPreferencesRequestDTO;
import com.streamhub.preferences.entity.UserCategoryPreference;
import com.streamhub.preferences.repository.UserCategoryPreferenceRepository;
import com.streamhub.preferences.service.UserCategoryPreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
//unit test suite for user category preferences and onboarding flows
public class UserCategoryPreferenceTest {

    @Mock
    private UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    private UserCategoryPreferenceService userCategoryPreferenceService;
    private UserCategoryPreferenceController userCategoryPreferenceController;

    private User testUser;
    private UUID cat1;
    private UUID cat2;
    private UUID cat3;

    @BeforeEach
    public void setup() {
        userCategoryPreferenceService = new UserCategoryPreferenceService(userCategoryPreferenceRepository, categoryRepository, userRepository);
        userCategoryPreferenceController = new UserCategoryPreferenceController(userCategoryPreferenceService, userRepository);

        testUser = new User();
        testUser.setUserId(UUID.randomUUID());
        testUser.setUsername("testuser");
        testUser.setHasSetInterests(false);

        cat1 = UUID.randomUUID();
        cat2 = UUID.randomUUID();
        cat3 = UUID.randomUUID();

        lenient().when(categoryRepository.count()).thenReturn(10L);
    }

    @Test
    //verifies needs onboarding returns true for fresh account and false once completed or for admin/moderator
    public void testNeedsOnboardingStatus() {
        testUser.setRole("USER");
        assertTrue(userCategoryPreferenceService.needsOnboarding(testUser));

        testUser.setHasSetInterests(true);
        assertFalse(userCategoryPreferenceService.needsOnboarding(testUser));

        //admin and moderator roles should never need onboarding even without interests set
        testUser.setHasSetInterests(false);
        testUser.setRole("ADMINISTRATOR");
        assertFalse(userCategoryPreferenceService.needsOnboarding(testUser));

        testUser.setRole("CONTENT_MODERATOR");
        assertFalse(userCategoryPreferenceService.needsOnboarding(testUser));

        testUser.setRole("ADMIN");
        assertFalse(userCategoryPreferenceService.needsOnboarding(testUser));

        testUser.setRole("MODERATOR");
        assertFalse(userCategoryPreferenceService.needsOnboarding(testUser));
    }

    @Test
    //verifies submitting fewer than 3 category preferences throws 400 bad request
    public void testSavePreferencesLessThanThreeThrowsBadRequest() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            userCategoryPreferenceService.saveCategoryPreferences(testUser, List.of(cat1, cat2));
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(Objects.requireNonNull(ex.getReason()).contains("At least 3"));

        ResponseStatusException exDuplicate = assertThrows(ResponseStatusException.class, () -> {
            userCategoryPreferenceService.saveCategoryPreferences(testUser, List.of(cat1, cat1, cat2));
        });
        assertEquals(HttpStatus.BAD_REQUEST, exDuplicate.getStatusCode());
    }

    @Test
    //verifies submitting non existent category throws 400 bad request
    public void testSavePreferencesNonExistentCategoryThrowsBadRequest() {
        when(categoryRepository.findById(cat1)).thenReturn(Optional.of(new Category("Cat1", "Desc1")));
        when(categoryRepository.findById(cat2)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            userCategoryPreferenceService.saveCategoryPreferences(testUser, List.of(cat1, cat2, cat3));
        });
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    //verifies submitting at least 3 valid categories saves preferences and marks onboarding complete
    public void testSavePreferencesSuccess() {
        Category c1 = new Category("Gaming", "Gaming desc");
        Category c2 = new Category("Music", "Music desc");
        Category c3 = new Category("Tech", "Tech desc");

        when(categoryRepository.findById(cat1)).thenReturn(Optional.of(c1));
        when(categoryRepository.findById(cat2)).thenReturn(Optional.of(c2));
        when(categoryRepository.findById(cat3)).thenReturn(Optional.of(c3));

        userCategoryPreferenceService.saveCategoryPreferences(testUser, List.of(cat1, cat2, cat3));

        verify(userCategoryPreferenceRepository, times(1)).deleteByUser(testUser);
        verify(userCategoryPreferenceRepository, times(3)).save(any(UserCategoryPreference.class));
        assertTrue(testUser.isHasSetInterests());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    //verifies check needs onboarding endpoint returns correct json response
    public void testCheckNeedsOnboardingEndpoint() {
        Principal principal = () -> "testuser";
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        ResponseEntity<Map<String, Boolean>> response = userCategoryPreferenceController.checkNeedsOnboarding(principal);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Boolean.TRUE, response.getBody().get("needsOnboarding"));
    }

    @Test
    //verifies category preferences submission endpoint succeeds
    public void testSaveCategoryPreferencesEndpoint() {
        Principal principal = () -> "testuser";
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(categoryRepository.findById(cat1)).thenReturn(Optional.of(new Category("Cat1", "Desc1")));
        when(categoryRepository.findById(cat2)).thenReturn(Optional.of(new Category("Cat2", "Desc2")));
        when(categoryRepository.findById(cat3)).thenReturn(Optional.of(new Category("Cat3", "Desc3")));

        CategoryPreferencesRequestDTO req = new CategoryPreferencesRequestDTO(List.of(cat1, cat2, cat3));
        ResponseEntity<Map<String, Object>> response = userCategoryPreferenceController.saveCategoryPreferences(principal, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(Boolean.TRUE, response.getBody().get("success"));
        assertTrue(testUser.isHasSetInterests());
    }
}
