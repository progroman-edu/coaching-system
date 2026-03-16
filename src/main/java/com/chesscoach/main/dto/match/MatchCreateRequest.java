// This DTO defines request payload fields for MatchCreate operations.
package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Setter
@Getter
public class MatchCreateRequest {

    @NotNull
    private LocalDate scheduledDate;

    @NotEmpty
    private List<Long> traineeIds;

    @NotNull
    private String format;

}

