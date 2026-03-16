// This DTO defines response payload fields for TraineePerformance endpoints.
package com.chesscoach.main.dto.analytics;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TraineePerformanceResponse {
    private Long traineeId;
    private String traineeName;
    private Integer currentRating;
    private Integer highestRating;
    private Integer matchesPlayed;
    private Integer wins;
    private Integer draws;
    private Integer losses;
    private Double attendancePercentage;

}

