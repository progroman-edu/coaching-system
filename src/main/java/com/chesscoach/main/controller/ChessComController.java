// This file exposes endpoints for Chess.com ratings and match history integration.
package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.chesscom.ChessComRatingResponse;
import com.chesscoach.main.dto.chesscom.ChessComSyncRatingResponse;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.service.ChessComService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CHESSCOM)
public class ChessComController {

    private final ChessComService chessComService;

    public ChessComController(ChessComService chessComService) {
        this.chessComService = chessComService;
    }

    @GetMapping("/{username}/rating")
    public ResponseEntity<ApiResponse<ChessComRatingResponse>> getRatings(
        @PathVariable String username,
        HttpServletRequest request
    ) {
        ChessComRatingResponse data = chessComService.getRatings(username);
        return ResponseEntity.ok(ApiResponse.ok("Chess.com ratings loaded", data, request.getRequestURI()));
    }

    @GetMapping("/{username}/match-history/archives")
    public ResponseEntity<ApiResponse<JsonNode>> getArchives(
        @PathVariable String username,
        HttpServletRequest request
    ) {
        JsonNode data = chessComService.getArchives(username);
        return ResponseEntity.ok(ApiResponse.ok("Chess.com archives loaded", data, request.getRequestURI()));
    }

    @GetMapping("/{username}/match-history/{year}/{month}")
    public ResponseEntity<ApiResponse<JsonNode>> getMonthlyHistory(
        @PathVariable String username,
        @PathVariable int year,
        @PathVariable int month,
        HttpServletRequest request
    ) {
        JsonNode data = chessComService.getMonthlyGames(username, year, month);
        return ResponseEntity.ok(ApiResponse.ok("Chess.com monthly history loaded", data, request.getRequestURI()));
    }

    @GetMapping("/{username}/match-history/all-modes")
    public ResponseEntity<ApiResponse<JsonNode>> getAllModesHistory(
        @PathVariable String username,
        @RequestParam(required = false) Integer limitArchives,
        HttpServletRequest request
    ) {
        JsonNode data = chessComService.getAllModeHistory(username, limitArchives);
        return ResponseEntity.ok(ApiResponse.ok("Chess.com all-mode history loaded", data, request.getRequestURI()));
    }

    @PostMapping("/trainees/{traineeId}/sync-rating")
    public ResponseEntity<ApiResponse<ChessComSyncRatingResponse>> syncRating(
        @PathVariable Long traineeId,
        @RequestParam(defaultValue = "rapid") String mode,
        HttpServletRequest request
    ) {
        ChessComSyncRatingResponse data = chessComService.syncTraineeRating(traineeId, mode);
        return ResponseEntity.ok(ApiResponse.ok("Trainee rating synced from Chess.com", data, request.getRequestURI()));
    }
}
