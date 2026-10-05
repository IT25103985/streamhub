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
        return targetStatus != null;
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
