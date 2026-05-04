// This DTO defines request payload fields for MatchGeneration operations.
package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MatchGenerationRequest {

    /**
     * List of trainee IDs to include in pairings.
     * - For Round Robin: specifies which trainees to pair
     * - For Swiss: when provided, scopes round tracking and pairings to these trainees
     */
    private List<Long> traineeIds;

    /**
     * Round number to generate.
     * Optional: if null or 0, the system will auto-calculate the maximum recommended rounds.
     * If provided explicitly, must be >= 1.
     */
    @Min(value = 0, message = "roundNumber must be null or >= 1")
    private Integer roundNumber;

}
