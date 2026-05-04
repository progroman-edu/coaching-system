// This service interface defines operations for Match workflows.
package com.chesscoach.main.service;

import com.chesscoach.main.dto.match.MatchCreateRequest;
import com.chesscoach.main.dto.match.MatchGenerationRequest;
import com.chesscoach.main.dto.match.MatchResultRequest;
import com.chesscoach.main.dto.match.MatchResultResponse;
import com.chesscoach.main.dto.match.MatchSummaryResponse;

import java.util.List;

public interface MatchService {
    MatchSummaryResponse createMatch(MatchCreateRequest request);

    List<MatchSummaryResponse> listMatches();

    List<MatchSummaryResponse> generateSwiss(MatchGenerationRequest request);

    List<MatchSummaryResponse> generateRoundRobin(MatchGenerationRequest request);

    MatchResultResponse recordResult(MatchResultRequest request);

    MatchSummaryResponse rollbackMatch(Long matchId);

    List<MatchSummaryResponse> getHistoryByTrainee(Long traineeId);

    /**
     * Calculate the maximum recommended rounds for a tournament format.
     * 
     * @param format "SWISS" or "ROUND_ROBIN"
     * @param participantCount the number of participants
     * @return maximum recommended rounds
     */
    int calculateMaxRoundsForFormat(String format, int participantCount);
}


