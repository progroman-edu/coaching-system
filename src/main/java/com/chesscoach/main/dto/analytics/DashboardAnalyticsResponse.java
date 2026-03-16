// This DTO defines response payload fields for DashboardAnalytics endpoints.
package com.chesscoach.main.dto.analytics;

import lombok.Getter;

@Getter
public class DashboardAnalyticsResponse {
    private Integer totalTrainees;
    private Double averageRating;
    private Double attendancePercentage;
    private Integer matchesPlayed;

    public void setTotalTrainees(Integer totalTrainees) {
        this.totalTrainees = totalTrainees;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }
}

