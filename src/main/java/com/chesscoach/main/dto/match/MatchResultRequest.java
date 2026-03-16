// This DTO defines request payload fields for MatchResult operations.
package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

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

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Double getWhiteScore() {
        return whiteScore;
    }

    public void setWhiteScore(Double whiteScore) {
        this.whiteScore = whiteScore;
    }

    public Double getBlackScore() {
        return blackScore;
    }

    public void setBlackScore(Double blackScore) {
        this.blackScore = blackScore;
    }
}

