package com.streamhub.settings.entity;

import com.streamhub.common.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "UserSettings")
public class UserSettings {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "watch_history_enabled", nullable = false)
    private boolean watchHistoryEnabled = true;

    @Column(name = "notify_social", nullable = false)
    private boolean notifySocial = true;

    @Column(name = "notify_moderation", nullable = false)
    private boolean notifyModeration = true;

    @Column(name = "notify_system", nullable = false)
    private boolean notifySystem = true;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public UserSettings() {
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
