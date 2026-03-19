// This DTO defines response payload fields for trainee rating history rows.
package com.chesscoach.main.dto.match;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
public class TraineeRatingHistoryResponse {

    private Long id;
    private Long traineeId;
    private Long matchHistoryId;
    private Integer oldRating;
    private Integer newRating;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}

