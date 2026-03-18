// This file defines the response payload for syncing trainee rating from Chess.com.
package com.chesscoach.main.dto.chesscom;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChessComSyncRatingResponse {
    private Long traineeId;
    private String chessUsername;
    private String mode;
    private Integer oldRating;
    private Integer newRating;

}
