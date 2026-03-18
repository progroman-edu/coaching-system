// This DTO defines request payload fields for MatchGeneration operations.
package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class MatchGenerationRequest {

    @NotEmpty
    private List<Long> traineeIds;

    @NotNull
    @Min(1)
    private Integer roundNumber;

}

