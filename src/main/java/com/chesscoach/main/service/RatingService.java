// This file contains project logic for RatingService.
package com.chesscoach.main.service;

import com.chesscoach.main.model.MatchResult;

public interface RatingService {
    void applyMatchResultRatingUpdate(MatchResult matchResult);
}

