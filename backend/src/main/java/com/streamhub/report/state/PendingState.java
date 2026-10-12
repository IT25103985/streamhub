package com.streamhub.report.state;

import com.streamhub.report.entity.Report;

//concrete state representing newly submitted report awaiting moderator triage
public class PendingState implements ReportState {

    @Override
    public String getName() {
        return "PENDING";
    }

    @Override
    public boolean canTransitionTo(String targetStatus) {
        if (targetStatus == null) return false;
        String s = targetStatus.trim().toUpperCase();
        return "REVIEWED".equals(s) || "UNDER_REVIEW".equals(s) || "IN_PROGRESS".equals(s) || "IN_REVIEW".equals(s) || "DISMISSED".equals(s);
    }

    @Override
    public void onEnter(Report report) {
        if (report != null) {
            report.setStatus("PENDING");
        }
    }

    @Override
    public void onExit(Report report) {
        //no-op on leaving pending state
    }
}
