package com.streamhub.report.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

//request body for reporting a video comment or user account
public class ReportCreateRequest {

    @NotBlank
    private String reportableType;

    @NotNull
    private UUID reportableId;

    @NotBlank
    private String reason;

    private String details;

    @AssertTrue(message = "Details are required when reason is OTHER")
    public boolean isDetailsValid() {
        if ("OTHER".equals(reason)) {
            return details != null && !details.trim().isEmpty();
        }
        return true;
    }

    public String getReportableType() {
        return reportableType;
    }

    public void setReportableType(String reportableType) {
        this.reportableType = reportableType;
    }

    public UUID getReportableId() {
        return reportableId;
    }

    public void setReportableId(UUID reportableId) {
        this.reportableId = reportableId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }
}
