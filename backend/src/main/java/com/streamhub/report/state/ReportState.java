package com.streamhub.report.state;

import com.streamhub.report.entity.Report;

//interface defining state lifecycle and transition contract for report moderation
public interface ReportState {

    //returns the canonical string representation of this state
    String getName();

    //checks whether transition to the target status string is permitted
    boolean canTransitionTo(String targetStatus);

    //lifecycle hook invoked when report transitions into this state
    void onEnter(Report report);

    //lifecycle hook invoked when report transitions out of this state
    void onExit(Report report);
}
