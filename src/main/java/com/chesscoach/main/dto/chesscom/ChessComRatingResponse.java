// This file defines normalized Chess.com rating data returned by integration endpoints.
package com.chesscoach.main.dto.chesscom;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChessComRatingResponse {
    private String username;
    
    // Current ratings
    private Integer rapid;
    private Integer blitz;
    private Integer bullet;
    
    // Best/Peak ratings
    private Integer rapidBest;
    private Integer blitzBest;
    private Integer bulletBest;
    
    private Integer puzzles;
    private Integer puzzleRush;

}
