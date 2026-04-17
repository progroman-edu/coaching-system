// This DTO defines response payload fields for trainee rating history rows.
package com.chesscoach.main.dto.match;

import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.time.OffsetDateTime;

@Setter
@Getter
public class TraineeRatingHistoryResponse {

    private Long id;
    private Long traineeId;
    @Nullable
    private Long matchHistoryId;
    @Nullable
    private Integer oldRating;
    @Nullable
    private Integer newRating;
    @Nullable
    private OffsetDateTime createdAt;
    @Nullable
    private OffsetDateTime updatedAt;
}

