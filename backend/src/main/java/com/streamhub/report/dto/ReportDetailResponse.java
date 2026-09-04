package com.streamhub.report.dto;

import java.util.Map;

//detailed response containing report info snapshot and target entity
public class ReportDetailResponse extends ReportResponse {
    private Map<String, Object> contentSnapshot;

    public ReportDetailResponse() {}

    public ReportDetailResponse(ReportResponse base, Map<String, Object> contentSnapshot) {
        this.setId(base.getId());
        this.setReporterName(base.getReporterName());
        this.setReportableType(base.getReportableType());
        this.setReportableId(base.getReportableId());
        this.setReason(base.getReason());
        this.setDetails(base.getDetails());
        this.setStatus(base.getStatus());
        this.setModeratorAction(base.getModeratorAction());
        this.setActionReason(base.getActionReason());
        this.setCreatedAt(base.getCreatedAt());
        this.setResolvedAt(base.getResolvedAt());
        this.setContentPreview(base.getContentPreview());
        this.contentSnapshot = contentSnapshot;
    }

    public Map<String, Object> getContentSnapshot() {
        return contentSnapshot;
    }

    public void setContentSnapshot(Map<String, Object> contentSnapshot) {
        this.contentSnapshot = contentSnapshot;
    }
}
