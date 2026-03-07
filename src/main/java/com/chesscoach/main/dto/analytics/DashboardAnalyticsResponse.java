// This DTO defines response payload fields for DashboardAnalytics endpoints.
package com.chesscoach.main.dto.analytics;

public class DashboardAnalyticsResponse {
    private Integer totalTrainees;
    private Double averageRating;
    private Double attendancePercentage;
    private Integer matchesPlayed;

    public Integer getTotalTrainees() {
        return totalTrainees;
    }

    public void setTotalTrainees(Integer totalTrainees) {
        this.totalTrainees = totalTrainees;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }
}

