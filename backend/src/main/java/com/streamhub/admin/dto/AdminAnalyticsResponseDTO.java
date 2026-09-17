package com.streamhub.admin.dto;

import java.util.List;

//dto holding aggregated admin metrics and recent signup numbers
public class AdminAnalyticsResponseDTO {

    private long totalUsers;
    private long totalVideos;
    private long totalCategories;
    private long pendingReports;
    private long totalCommentsAndLikes;
    private List<SignupDayDTO> signupsLast7Days;

    public AdminAnalyticsResponseDTO() {}

    public AdminAnalyticsResponseDTO(long totalUsers, long totalVideos, long totalCategories,
                                     long pendingReports, long totalCommentsAndLikes,
                                     List<SignupDayDTO> signupsLast7Days) {
        this.totalUsers = totalUsers;
        this.totalVideos = totalVideos;
        this.totalCategories = totalCategories;
        this.pendingReports = pendingReports;
        this.totalCommentsAndLikes = totalCommentsAndLikes;
        this.signupsLast7Days = signupsLast7Days;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public long getTotalVideos() {
        return totalVideos;
    }

    public void setTotalVideos(long totalVideos) {
        this.totalVideos = totalVideos;
    }

    public long getTotalCategories() {
        return totalCategories;
    }

    public void setTotalCategories(long totalCategories) {
        this.totalCategories = totalCategories;
    }

    public long getPendingReports() {
        return pendingReports;
    }

    public void setPendingReports(long pendingReports) {
        this.pendingReports = pendingReports;
    }

    public long getTotalCommentsAndLikes() {
        return totalCommentsAndLikes;
    }

    public void setTotalCommentsAndLikes(long totalCommentsAndLikes) {
        this.totalCommentsAndLikes = totalCommentsAndLikes;
    }

    public List<SignupDayDTO> getSignupsLast7Days() {
        return signupsLast7Days;
    }

    public void setSignupsLast7Days(List<SignupDayDTO> signupsLast7Days) {
        this.signupsLast7Days = signupsLast7Days;
    }

    public static class SignupDayDTO {
        private String date;
        private long count;

        public SignupDayDTO() {}

        public SignupDayDTO(String date, long count) {
            this.date = date;
            this.count = count;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public long getCount() {
            return count;
        }

        public void setCount(long count) {
            this.count = count;
        }
    }
}
