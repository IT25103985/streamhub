package com.streamhub.report.dto;

//request payload for reversing a previously taken moderation action
public class ReportReverseActionRequest {

    private String reason;

    public ReportReverseActionRequest() {}

    public ReportReverseActionRequest(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
