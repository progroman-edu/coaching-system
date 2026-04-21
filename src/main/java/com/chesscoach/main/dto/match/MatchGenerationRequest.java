// This DTO defines request payload fields for MatchGeneration operations.
package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MatchGenerationRequest {

    /**
     * List of trainee IDs to include in pairings.
     * - For Round Robin: specifies which trainees to pair
     * - For Swiss: ignored (all trainees are automatically included)
     */
    private List<Long> traineeIds;

    @NotNull
    @Min(1)
    private Integer roundNumber;

}

