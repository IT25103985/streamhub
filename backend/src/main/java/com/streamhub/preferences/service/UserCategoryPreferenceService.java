package com.streamhub.preferences.service;

import com.streamhub.auth.repository.UserRepository;
import com.streamhub.category.entity.Category;
import com.streamhub.category.repository.CategoryRepository;
import com.streamhub.common.entity.User;
import com.streamhub.preferences.entity.UserCategoryPreference;
import com.streamhub.preferences.repository.UserCategoryPreferenceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
//service managing onboarding interests preferences and onboarding completion status
public class UserCategoryPreferenceService {

    private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public UserCategoryPreferenceService(UserCategoryPreferenceRepository userCategoryPreferenceRepository,
                                         CategoryRepository categoryRepository,
                                         UserRepository userRepository) {
        this.userCategoryPreferenceRepository = userCategoryPreferenceRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    //determines if the user still requires category interests onboarding
    public boolean needsOnboarding(User user) {
        if (user == null) {
            return false;
        }
        String role = user.getRole();
        if (role != null && (!"USER".equalsIgnoreCase(role) || "ADMINISTRATOR".equalsIgnoreCase(role) || "CONTENT_MODERATOR".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role) || "MODERATOR".equalsIgnoreCase(role))) {
            return false;
        }
        return !user.isHasSetInterests();
    }

    @Transactional
    //saves at least 3 distinct category preferences and marks onboarding complete
    public void saveCategoryPreferences(User user, List<UUID> categoryIds) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }

        if (categoryIds == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category preferences list cannot be null");
        }

        List<UUID> distinctCategoryIds = categoryIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        int totalAvailableCategories = (int) categoryRepository.count();
        int minRequired = Math.min(3, totalAvailableCategories);
        if (distinctCategoryIds.size() < minRequired || (minRequired > 0 && distinctCategoryIds.isEmpty())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least " + minRequired + " distinct category preference(s) are required");
        }

        List<Category> categories = new ArrayList<>();
        for (UUID catId : distinctCategoryIds) {
            Category category = categoryRepository.findById(catId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category not found with id " + catId));
            categories.add(category);
        }

        //remove previous preferences for clean replacement
        userCategoryPreferenceRepository.deleteByUser(user);

        //persist new category preferences
        for (Category category : categories) {
            UserCategoryPreference preference = new UserCategoryPreference(user, category);
            userCategoryPreferenceRepository.save(preference);
        }

        //mark onboarding completed
        user.setHasSetInterests(true);
        userRepository.save(user);
    }

    @Transactional
    //marks onboarding as completed without saving category preferences
    public void skipOnboarding(User user) {
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User is not authenticated");
        }
        user.setHasSetInterests(true);
        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    //retrieves preferred category ids saved by user
    public List<UUID> getPreferredCategoryIds(User user) {
        if (user == null) {
            return new ArrayList<>();
        }
        return userCategoryPreferenceRepository.findCategoryIdsByUser(user);
    }
}
