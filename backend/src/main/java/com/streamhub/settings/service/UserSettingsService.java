package com.streamhub.settings.service;

import com.streamhub.common.entity.User;
import com.streamhub.settings.entity.UserSettings;
import com.streamhub.settings.repository.UserSettingsRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class UserSettingsService {

    private final UserSettingsRepository userSettingsRepository;
    private final EntityManager entityManager;

    public UserSettingsService(UserSettingsRepository userSettingsRepository, EntityManager entityManager) {
        this.userSettingsRepository = userSettingsRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public UserSettings getOrCreate(UUID userId) {
        return userSettingsRepository.findById(userId)
                .orElseGet(() -> {
                    User userRef = entityManager.getReference(User.class, userId);
                    UserSettings settings = new UserSettings();
                    settings.setUser(userRef);
                    return userSettingsRepository.save(settings);
                });
    }

    @Transactional
    public UserSettings update(UUID userId, Map<String, Boolean> updates) {
        UserSettings settings = getOrCreate(userId);
        if (updates.containsKey("watchHistoryEnabled")) {
            settings.setWatchHistoryEnabled(updates.get("watchHistoryEnabled"));
        }
        if (updates.containsKey("notifySocial")) {
            settings.setNotifySocial(updates.get("notifySocial"));
        }
        if (updates.containsKey("notifyModeration")) {
            settings.setNotifyModeration(updates.get("notifyModeration"));
        }
        if (updates.containsKey("notifySystem")) {
            settings.setNotifySystem(updates.get("notifySystem"));
        }
        return userSettingsRepository.save(settings);
    }
}
