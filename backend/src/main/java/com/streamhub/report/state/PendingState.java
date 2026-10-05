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
        return targetStatus != null;
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
