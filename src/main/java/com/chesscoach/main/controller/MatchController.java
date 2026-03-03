// This file contains project logic for MatchController.
package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.dto.match.MatchCreateRequest;
import com.chesscoach.main.dto.match.MatchGenerationRequest;
import com.chesscoach.main.dto.match.MatchResultRequest;
import com.chesscoach.main.dto.match.MatchSummaryResponse;
import com.chesscoach.main.service.MatchService;
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
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MatchSummaryResponse>> createMatch(
        @Valid @RequestBody MatchCreateRequest requestBody,
        HttpServletRequest request
    ) {
        MatchSummaryResponse data = matchService.createMatch(requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("Match schedule created", data, request.getRequestURI()));
    }

    @PostMapping("/generate/swiss")
    public ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> generateSwiss(
        @Valid @RequestBody MatchGenerationRequest requestBody,
        HttpServletRequest request
    ) {
        List<MatchSummaryResponse> data = matchService.generateSwiss(requestBody);
        return ResponseEntity.ok(ApiResponse.ok("Swiss pairings generated", data, request.getRequestURI()));
    }

    @PostMapping("/generate/round-robin")
    public ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> generateRoundRobin(
        @Valid @RequestBody MatchGenerationRequest requestBody,
        HttpServletRequest request
    ) {
        List<MatchSummaryResponse> data = matchService.generateRoundRobin(requestBody);
        return ResponseEntity.ok(ApiResponse.ok("Round Robin pairings generated", data, request.getRequestURI()));
    }

    @PostMapping("/result")
    public ResponseEntity<ApiResponse<MatchResultRequest>> recordResult(
        @Valid @RequestBody MatchResultRequest requestBody,
        HttpServletRequest request
    ) {
        MatchResultRequest data = matchService.recordResult(requestBody);
        return ResponseEntity.ok(ApiResponse.ok("Match result recorded", data, request.getRequestURI()));
    }

    @GetMapping("/history/{traineeId}")
    public ResponseEntity<ApiResponse<List<MatchSummaryResponse>>> getHistoryByTrainee(
        @PathVariable Long traineeId,
        HttpServletRequest request
    ) {
        List<MatchSummaryResponse> data = matchService.getHistoryByTrainee(traineeId);
        return ResponseEntity.ok(ApiResponse.ok("Match history for trainee " + traineeId, data, request.getRequestURI()));
    }
}

