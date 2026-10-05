package com.streamhub.report.state;

import com.streamhub.report.entity.Report;

import java.time.LocalDateTime;

//fallback state handling arbitrary or custom status strings preserving backward compatibility
public class GenericReportState implements ReportState {

    private final String name;

    public GenericReportState(String name) {
        this.name = name != null ? name : "PENDING";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean canTransitionTo(String targetStatus) {
        return targetStatus != null;
    }

    @Override
    public void onEnter(Report report) {
        if (report != null) {
            report.setStatus(name);
            if ("RESOLVED".equalsIgnoreCase(name)) {
                report.setResolvedAt(LocalDateTime.now());
            }
        }
    }

    @Override
    public void onExit(Report report) {
        //no-op
    }
}
