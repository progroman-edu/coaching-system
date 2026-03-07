// This service interface defines operations for Rating workflows.
package com.chesscoach.main.service;

import com.chesscoach.main.model.MatchResult;

public interface RatingService {
    void applyMatchResultRatingUpdate(MatchResult matchResult);
}

