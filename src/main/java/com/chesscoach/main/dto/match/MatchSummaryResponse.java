// This DTO defines response payload fields for MatchSummary endpoints.
package com.chesscoach.main.dto.match;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class MatchSummaryResponse {
    private Long matchId;
    private LocalDate scheduledDate;
    private String format;
    private String whitePlayer;
    private String blackPlayer;
    private String result;

}

