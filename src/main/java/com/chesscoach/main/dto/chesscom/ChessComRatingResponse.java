// This file defines normalized Chess.com rating data returned by integration endpoints.
package com.chesscoach.main.dto.chesscom;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChessComRatingResponse {
    private String username;
    private Integer rapid;
    private Integer blitz;
    private Integer bullet;
    private Integer puzzles;
    private Integer puzzleRush;

}
