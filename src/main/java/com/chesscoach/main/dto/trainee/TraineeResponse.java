// This DTO defines response payload fields for Trainee endpoints.
package com.chesscoach.main.dto.trainee;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class TraineeResponse {
    private Long id;
    private String name;
    private Integer age;
    private String address;
    private String gradeLevel;
    private String courseStrand;
    private Integer currentRating;
    private String currentRatingMode;
    private Integer highestRapidRating;
    private Integer highestBlitzRating;
    private Integer highestBulletRating;
    private Integer ranking;
    private String photoPath;
    private String chessUsername;

}

