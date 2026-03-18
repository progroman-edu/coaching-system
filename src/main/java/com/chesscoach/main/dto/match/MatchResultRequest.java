// This DTO defines request payload fields for MatchResult operations.
package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MatchResultRequest {

    @NotNull
    private Long matchId;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double whiteScore;

    @NotNull
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double blackScore;

}

