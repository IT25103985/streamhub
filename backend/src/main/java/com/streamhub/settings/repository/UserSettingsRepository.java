package com.streamhub.settings.repository;

import com.streamhub.settings.entity.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
//repository for managing user notification and watch history settings
public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID> {
}
