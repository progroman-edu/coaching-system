// This DTO defines response payload fields for Trainee endpoints.
package com.chesscoach.main.dto.trainee;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
public class TraineeResponse {
    private Long id;
    private String name;
    private Integer age;
    private String address;
    private String gradeLevel;
    private String department;
    private Integer blitzCurrentRating;
    private Integer blitzHighestRating;
    private Integer bulletCurrentRating;
    private Integer bulletHighestRating;
    private Integer rapidCurrentRating;
    private Integer rapidHighestRating;
    private Integer latestRatingChange;
    private Double attendancePercentageLast30Days;
    private OffsetDateTime lastActivityAt;
    private Integer ranking;
    private String photoPath;
    private String chessUsername;

}
