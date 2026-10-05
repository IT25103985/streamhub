package com.streamhub.report.entity;

import com.streamhub.common.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report")
//database entity tracking reported content status and moderator actions
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(name = "reportable_type", length = 20)
    private String reportableType; //video, comment, playlist, profile

    @Column(name = "reportable_id")
    private UUID reportableId;

    @Column(name = "reason", length = 50)
    private String reason;

    @Column(name = "details", columnDefinition = "NVARCHAR(1000)")
    private String details;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private User moderator;

    @Column(name = "moderator_action", length = 20)
    private String moderatorAction;

    @Column(name = "action_reason", columnDefinition = "NVARCHAR(1000)")
    private String actionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public Report() {}

    public Report(User reporter, String reportableType, UUID reportableId, String reason, String details) {
        this.reporter = reporter;
        this.reportableType = reportableType;
        this.reportableId = reportableId;
        this.reason = reason;
        this.details = details;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public User getReporter() { return reporter; }
    public void setReporter(User reporter) { this.reporter = reporter; }

    public String getReportableType() { return reportableType; }
    public void setReportableType(String reportableType) { this.reportableType = reportableType; }

    public UUID getReportableId() { return reportableId; }
    public void setReportableId(UUID reportableId) { this.reportableId = reportableId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    //returns the state object corresponding to the current status
    public com.streamhub.report.state.ReportState getCurrentState() {
        return com.streamhub.report.state.ReportStateFactory.getState(this.status);
    }

    //transitions the report lifecycle state executing exit and entry hooks
    public void transitionTo(com.streamhub.report.state.ReportState targetState) {
        if (targetState == null) return;
        com.streamhub.report.state.ReportState current = getCurrentState();
        if (current != null) {
            current.onExit(this);
        }
        targetState.onEnter(this);
    }

    public User getModerator() { return moderator; }
    public void setModerator(User moderator) { this.moderator = moderator; }

    public String getModeratorAction() { return moderatorAction; }
    public void setModeratorAction(String moderatorAction) { this.moderatorAction = moderatorAction; }

    public String getActionReason() { return actionReason; }
    public void setActionReason(String actionReason) { this.actionReason = actionReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
}

