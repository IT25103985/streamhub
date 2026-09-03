package com.streamhub.report.dto;

import com.streamhub.report.entity.ReportStatus;
import com.streamhub.report.entity.ReportTargetType;

import java.time.LocalDateTime;
import java.util.UUID;

//data transfer object for report listings in moderator view
public class ReportDTO {

    private UUID reportId;
    private String reporterUsername;
    private ReportTargetType targetType;
    private UUID targetId;
    private String reason;
    private ReportStatus status;
    private LocalDateTime createdAt;

    public ReportDTO() {}

    public ReportDTO(UUID reportId, String reporterUsername, ReportTargetType targetType, UUID targetId, String reason, ReportStatus status, LocalDateTime createdAt) {
        this.reportId = reportId;
        this.reporterUsername = reporterUsername;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
    }

    public UUID getReportId() { return reportId; }
    public void setReportId(UUID reportId) { this.reportId = reportId; }

    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }

    public ReportTargetType getTargetType() { return targetType; }
    public void setTargetType(ReportTargetType targetType) { this.targetType = targetType; }

    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
