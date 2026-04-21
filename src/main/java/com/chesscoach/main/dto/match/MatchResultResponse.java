// This DTO defines response payload for MatchResult operations.
package com.chesscoach.main.dto.match;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResultResponse {

    private Long matchId;

    private String resultType;

    private Double whiteScore;

    private Double blackScore;

    private Double whiteRatingDelta;

    private Double blackRatingDelta;

    private LocalDateTime recordedAt;

    private String message;

}
