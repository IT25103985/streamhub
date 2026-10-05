package com.streamhub.report.state;

//factory resolving state instances for corresponding status strings
public final class ReportStateFactory {

    private static final ReportState PENDING = new PendingState();
    private static final ReportState REVIEWED = new ReviewedState();
    private static final ReportState RESOLVED = new ResolvedState();

    private ReportStateFactory() {}

    //resolves singleton or generic state object for given status identifier
    public static ReportState getState(String status) {
        if (status == null) {
            return PENDING;
        }
        String normalized = status.trim().toUpperCase();
        switch (normalized) {
            case "PENDING":
                return PENDING;
            case "REVIEWED":
            case "UNDER_REVIEW":
            case "IN_PROGRESS":
                return REVIEWED;
            case "RESOLVED":
                return RESOLVED;
            default:
                return new GenericReportState(status);
        }
    }

    public static ReportState getPendingState() {
        return PENDING;
    }

    public static ReportState getReviewedState() {
        return REVIEWED;
    }

    public static ReportState getResolvedState() {
        return RESOLVED;
    }
}
