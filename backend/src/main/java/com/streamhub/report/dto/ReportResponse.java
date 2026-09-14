package com.streamhub.report.dto;

import com.streamhub.report.entity.Report;
import java.time.LocalDateTime;
import java.util.UUID;

//standard response object returned after report creation or action
public class ReportResponse {
    private UUID id;
    private String reporterName;
    private String reportableType;
    private UUID reportableId;
    private String reason;
    private String details;
    private String status;
    private String moderatorAction;
    private String actionReason;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private String contentPreview;

    public ReportResponse() {}

    public ReportResponse(Report report, String contentPreview) {
        this.id = report.getId();
        this.reporterName = report.getReporter().getUsername();
        this.reportableType = report.getReportableType();
        this.reportableId = report.getReportableId();
        this.reason = report.getReason();
        this.details = report.getDetails();
        this.status = report.getStatus();
        this.moderatorAction = report.getModeratorAction();
        this.actionReason = report.getActionReason();
        this.createdAt = report.getCreatedAt();
        this.resolvedAt = report.getResolvedAt();
        this.contentPreview = contentPreview;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
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
    public String getModeratorAction() { return moderatorAction; }
    public void setModeratorAction(String moderatorAction) { this.moderatorAction = moderatorAction; }
    public String getActionReason() { return actionReason; }
    public void setActionReason(String actionReason) { this.actionReason = actionReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(LocalDateTime resolvedAt) { this.resolvedAt = resolvedAt; }
    public String getContentPreview() { return contentPreview; }
    public void setContentPreview(String contentPreview) { this.contentPreview = contentPreview; }
}
