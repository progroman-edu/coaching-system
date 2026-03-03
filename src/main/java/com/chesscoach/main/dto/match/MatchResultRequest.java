// This file contains project logic for MatchResultRequest.
package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.NotNull;

public class MatchResultRequest {

    @NotNull
    private Long matchId;

    @NotNull
    private Long whiteTraineeId;

    @NotNull
    private Long blackTraineeId;

    @NotNull
    private Double whiteScore;

    @NotNull
    private Double blackScore;

    public Long getMatchId() {
        return matchId;
    }

    public void setMatchId(Long matchId) {
        this.matchId = matchId;
    }

    public Long getWhiteTraineeId() {
        return whiteTraineeId;
    }

    public void setWhiteTraineeId(Long whiteTraineeId) {
        this.whiteTraineeId = whiteTraineeId;
    }

    public Long getBlackTraineeId() {
        return blackTraineeId;
    }

    public void setBlackTraineeId(Long blackTraineeId) {
        this.blackTraineeId = blackTraineeId;
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

