package com.streamhub.report.state;

import com.streamhub.report.entity.Report;

//concrete state representing report actively under moderator examination
public class ReviewedState implements ReportState {

    @Override
    public String getName() {
        return "REVIEWED";
    }

    @Override
    public boolean canTransitionTo(String targetStatus) {
        if (targetStatus == null) return false;
        String s = targetStatus.trim().toUpperCase();
        return "RESOLVED".equals(s) || "DISMISSED".equals(s) || "REJECTED".equals(s) || "PENDING".equals(s);
    }

    @Override
    public void onEnter(Report report) {
        if (report != null) {
            report.setStatus("REVIEWED");
        }
    }

    @Override
    public void onExit(Report report) {
        //no-op on leaving reviewed state
    }
}
