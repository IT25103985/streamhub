package com.streamhub.settings.dto;

import com.streamhub.settings.entity.UserSettings;
import java.util.UUID;

//data transfer object transferring user notification and watch history preferences
public class UserSettingsDTO {
    private UUID userId;
    private boolean watchHistoryEnabled;
    private boolean notifySocial;
    private boolean notifyModeration;
    private boolean notifySystem;

    public UserSettingsDTO() {
    }

    public UserSettingsDTO(UserSettings settings) {
        this.userId = settings.getUserId();
        this.watchHistoryEnabled = settings.isWatchHistoryEnabled();
        this.notifySocial = settings.isNotifySocial();
        this.notifyModeration = settings.isNotifyModeration();
        this.notifySystem = settings.isNotifySystem();
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public boolean isWatchHistoryEnabled() {
        return watchHistoryEnabled;
    }

    public void setWatchHistoryEnabled(boolean watchHistoryEnabled) {
        this.watchHistoryEnabled = watchHistoryEnabled;
    }

    public boolean isNotifySocial() {
        return notifySocial;
    }

    public void setNotifySocial(boolean notifySocial) {
        this.notifySocial = notifySocial;
    }

    public boolean isNotifyModeration() {
        return notifyModeration;
    }

    public void setNotifyModeration(boolean notifyModeration) {
        this.notifyModeration = notifyModeration;
    }

    public boolean isNotifySystem() {
        return notifySystem;
    }

    public void setNotifySystem(boolean notifySystem) {
        this.notifySystem = notifySystem;
    }
}
