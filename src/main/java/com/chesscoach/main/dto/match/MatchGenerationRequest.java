// This DTO defines request payload fields for MatchGeneration operations.
package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class MatchGenerationRequest {

    @NotEmpty
    private List<Long> traineeIds;

    @NotNull
    @Min(1)
    private Integer roundNumber;

    public List<Long> getTraineeIds() {
        return traineeIds;
    }

    public void setTraineeIds(List<Long> traineeIds) {
        this.traineeIds = traineeIds;
    }

    public Integer getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(Integer roundNumber) {
        this.roundNumber = roundNumber;
    }
}

