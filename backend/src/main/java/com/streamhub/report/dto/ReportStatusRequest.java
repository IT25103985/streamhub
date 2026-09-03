package com.streamhub.report.dto;

import jakarta.validation.constraints.NotBlank;

//payload for updating report status to in progress or resolved
public class ReportStatusRequest {

    @NotBlank
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
