package com.streamhub.report.dto;

import com.streamhub.report.entity.ReportStatus;
import jakarta.validation.constraints.NotNull;

//dto for updating resolution state of a moderation report
public class UpdateReportStatusDTO {
    
    @NotNull(message = "Status is required")
    private ReportStatus status;

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
}
