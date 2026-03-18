// This DTO defines response payload fields for DashboardAnalytics endpoints.
package com.chesscoach.main.dto.analytics;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class DashboardAnalyticsResponse {
    private Integer totalTrainees;
    private Double averageRating;
    private Double attendancePercentage;
    private Integer matchesPlayed;

}

