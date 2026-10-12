package com.streamhub.report.state;

import com.streamhub.report.entity.Report;

import java.time.LocalDateTime;

//concrete state representing finalized report where moderator action or closure occurred
public class ResolvedState implements ReportState {

    @Override
    public String getName() {
        return "RESOLVED";
    }

    @Override
    public boolean canTransitionTo(String targetStatus) {
        if (targetStatus == null) return false;
        String s = targetStatus.trim().toUpperCase();
        return "PENDING".equals(s) || "UNDER_REVIEW".equals(s) || "REVIEWED".equals(s);
    }

    @Override
    public void onEnter(Report report) {
        if (report != null) {
            report.setStatus("RESOLVED");
            report.setResolvedAt(LocalDateTime.now());
        }
    }

    @Override
    public void onExit(Report report) {
        //no-op on leaving resolved state
    }
}
