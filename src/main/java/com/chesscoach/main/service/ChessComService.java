// This file declares operations for Chess.com PubAPI integration.
package com.chesscoach.main.service;

import com.chesscoach.main.dto.chesscom.ChessComRatingResponse;
import com.chesscoach.main.dto.chesscom.ChessComSyncRatingResponse;
import com.fasterxml.jackson.databind.JsonNode;

public interface ChessComService {
    ChessComRatingResponse getRatings(String username);

    JsonNode getArchives(String username);

    JsonNode getMonthlyGames(String username, int year, int month);

    JsonNode getAllModeHistory(String username, Integer limitArchives);

    ChessComSyncRatingResponse syncTraineeRating(Long traineeId, String mode);
}
