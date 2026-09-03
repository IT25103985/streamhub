package com.streamhub.report.dto;

import jakarta.validation.constraints.NotBlank;

//moderator payload specifying action type and justification
public class ReportActionRequest {

    @NotBlank
    private String action;

    @NotBlank
    private String actionReason;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionReason() {
        return actionReason;
    }

    public void setActionReason(String actionReason) {
        this.actionReason = actionReason;
    }
}
