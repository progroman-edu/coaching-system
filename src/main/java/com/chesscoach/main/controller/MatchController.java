// This controller exposes HTTP endpoints for Match workflows.
package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.dto.match.MatchCreateRequest;
import com.chesscoach.main.dto.match.MatchGenerationRequest;
import com.chesscoach.main.dto.match.MatchResultRequest;
import com.chesscoach.main.dto.match.MatchSummaryResponse;
import com.chesscoach.main.dto.match.TraineeRatingHistoryResponse;
import com.chesscoach.main.service.MatchService;
import com.chesscoach.main.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.MATCHES)
@Tag(name = "Matches", description = "APIs for managing chess matches, pairings, and results")
public class MatchController {

    private final MatchService matchService;
    private final RatingService ratingService;

    public MatchController(MatchService matchService, RatingService ratingService) {
        this.matchService = matchService;
        this.ratingService = ratingService;
    }

    @PostMapping
    @Operation(summary = "Create a new match", description = "Create a new match between specified trainees")
    public ResponseEntity<ApiResponse<MatchSummaryResponse>> createMatch(
        @Valid @RequestBody MatchCreateRequest requestBody,
        HttpServletRequest request
    ) {
        MatchSummaryResponse data = matchService.createMatch(requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("Match schedule created", data, request.getRequestURI()));
    }

    @PostMapping("/generate/swiss")
    @Operation(summary = "Generate Swiss pairings", description = "Generate Swiss system pairings for a round")
    public ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> generateSwiss(
        @Valid @RequestBody MatchGenerationRequest requestBody,
        HttpServletRequest request
    ) {
        List<MatchSummaryResponse> data = matchService.generateSwiss(requestBody);
        return ResponseEntity.ok(ApiResponse.ok("Swiss pairings generated", data, request.getRequestURI()));
    }

    @PostMapping("/generate/round-robin")
    @Operation(summary = "Generate Round Robin pairings", description = "Generate Round Robin pairings for a round")
    public ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> generateRoundRobin(
        @Valid @RequestBody MatchGenerationRequest requestBody,
        HttpServletRequest request
    ) {
        List<MatchSummaryResponse> data = matchService.generateRoundRobin(requestBody);
        return ResponseEntity.ok(ApiResponse.ok("Round Robin pairings generated", data, request.getRequestURI()));
    }

    @PostMapping("/result")
    @Operation(summary = "Record match result", description = "Record the result of a completed match")
    public ResponseEntity<ApiResponse<MatchResultRequest>> recordResult(
        @Valid @RequestBody MatchResultRequest requestBody,
        HttpServletRequest request
    ) {
        MatchResultRequest data = matchService.recordResult(requestBody);
        return ResponseEntity.ok(ApiResponse.ok("Match result recorded", data, request.getRequestURI()));
    }

    @GetMapping("/history/{traineeId}")
    @Operation(summary = "Get match history", description = "Get all matches involving a specific trainee")
    public ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> getHistoryByTrainee(
        @PathVariable Long traineeId,
        HttpServletRequest request
    ) {
        List<MatchSummaryResponse> data = matchService.getHistoryByTrainee(traineeId);
        return ResponseEntity.ok(ApiResponse.ok("Match history for trainee " + traineeId, data, request.getRequestURI()));
    }

    @GetMapping("/history/{traineeId}/ratings")
    @Operation(summary = "Get rating history", description = "Get rating change history for a specific trainee")
    public ResponseEntity<ApiResponse<List<TraineeRatingHistoryResponse>>> getRatingHistoryByTrainee(
        @PathVariable Long traineeId,
        HttpServletRequest request
    ) {
        List<TraineeRatingHistoryResponse> data = ratingService.getRatingHistoryByTrainee(traineeId);
        return ResponseEntity.ok(ApiResponse.ok("Rating history for trainee " + traineeId, data, request.getRequestURI()));
    }
}

