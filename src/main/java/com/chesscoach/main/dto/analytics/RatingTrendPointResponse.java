// This DTO defines response payload fields for RatingTrendPoint endpoints.
package com.chesscoach.main.dto.analytics;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
public class RatingTrendPointResponse {
    private OffsetDateTime timestamp;
    private Integer rating;
    private Integer change;

}

