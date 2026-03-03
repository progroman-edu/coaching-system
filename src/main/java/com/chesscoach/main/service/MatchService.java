// This file contains project logic for MatchService.
package com.chesscoach.main.service;

import com.chesscoach.main.dto.match.MatchCreateRequest;
import com.chesscoach.main.dto.match.MatchGenerationRequest;
import com.chesscoach.main.dto.match.MatchResultRequest;
import com.chesscoach.main.dto.match.MatchSummaryResponse;

import java.util.List;

public interface MatchService {
    MatchSummaryResponse createMatch(MatchCreateRequest request);

    List<MatchSummaryResponse> generateSwiss(MatchGenerationRequest request);

    List<MatchSummaryResponse> generateRoundRobin(MatchGenerationRequest request);

    MatchResultRequest recordResult(MatchResultRequest request);

    List<MatchSummaryResponse> getHistoryByTrainee(Long traineeId);
}

