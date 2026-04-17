// This DTO defines response payload fields for Trainee endpoints.
package com.chesscoach.main.dto.trainee;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;
import java.util.Optional;

@Setter
@Getter
public class TraineeResponse {
    private Long id;
    private String name;
    @Nullable
    private Integer age;
    @Nullable
    private String address;
    @Nullable
    private String gradeLevel;
    @Nullable
    private String department;
    @Nullable
    private Integer blitzCurrentRating;
    @Nullable
    private Integer blitzHighestRating;
    @Nullable
    private Integer bulletCurrentRating;
    @Nullable
    private Integer bulletHighestRating;
    @Nullable
    private Integer rapidCurrentRating;
    @Nullable
    private Integer rapidHighestRating;
    @Nullable
    private Integer latestRatingChange;
    @Nullable
    private Double attendancePercentageLast30Days;
    @Nullable
    private OffsetDateTime lastActivityAt;
    @Nullable
    private Integer ranking;
    @Nullable
    private String photoPath;
    @Nullable
    private String chessUsername;

}
