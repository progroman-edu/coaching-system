// This service implements Swiss tournament scoring and tiebreaker logic.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.MatchResultType;
import com.chesscoach.main.model.MatchParticipant;
import com.chesscoach.main.repository.MatchResultRepository;
import com.chesscoach.main.repository.MatchParticipantRepository;
import com.chesscoach.main.service.SwissScorer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SwissScorerImpl implements SwissScorer {

    private static final Logger log = LoggerFactory.getLogger(SwissScorerImpl.class);

    private final MatchResultRepository matchResultRepository;
    private final MatchParticipantRepository matchParticipantRepository;

    public SwissScorerImpl(MatchResultRepository matchResultRepository, MatchParticipantRepository matchParticipantRepository) {
        this.matchResultRepository = matchResultRepository;
        this.matchParticipantRepository = matchParticipantRepository;
    }

    @Override
    public double getPlayerScore(Long traineeId, int upToRoundNumber) {
        List<MatchResult> results = getPlayerMatchResultsUpToRound(traineeId, upToRoundNumber);
        return results.stream()
            .mapToDouble(result -> getPointsForMatch(result, traineeId))
            .sum();
    }

    @Override
    public double getPlayerBuchholzScore(Long traineeId, int upToRoundNumber) {
        // Get all opponents this player faced up to the round
        List<Long> opponentIds = getOpponentIds(traineeId, upToRoundNumber);

        // For each opponent, calculate their final score, then sum
        double buchholzSum = 0.0;
        for (Long opponentId : opponentIds) {
            double opponentScore = getPlayerScore(opponentId, upToRoundNumber);
            buchholzSum += opponentScore;
        }

        log.debug("Trainee {} Buchholz score: {} (faced {} opponents)", traineeId, buchholzSum, opponentIds.size());
        return buchholzSum;
    }

    @Override
    public List<MatchResult> getPlayerMatchResultsUpToRound(Long traineeId, int upToRoundNumber) {
        // Query all match results where trainee is white or black
        List<MatchResult> allResults = matchResultRepository.findByWhiteTraineeIdOrBlackTraineeIdOrderByPlayedAtDesc(traineeId, traineeId);

        // Filter by swiss_round_number <= upToRoundNumber
        return allResults.stream()
            .filter(result -> {
                Integer roundNumber = getSwissRoundFromResult(result, traineeId);
                return roundNumber != null && roundNumber <= upToRoundNumber;
            })
            .collect(Collectors.toList());
    }

    @Override
    public boolean havePlayedEachOther(Long traineeId1, Long traineeId2, int upToRoundNumber) {
        List<MatchResult> results = matchResultRepository.findByWhiteTraineeIdOrBlackTraineeIdOrderByPlayedAtDesc(traineeId1, traineeId1);

        return results.stream()
            .anyMatch(result -> {
                boolean bothPlayed = (result.getWhiteTrainee().getId().equals(traineeId1) && result.getBlackTrainee().getId().equals(traineeId2))
                    || (result.getWhiteTrainee().getId().equals(traineeId2) && result.getBlackTrainee().getId().equals(traineeId1));

                if (!bothPlayed) {
                    return false;
                }

                // Check round number
                Integer roundNumber = getSwissRoundFromResult(result, traineeId1);
                return roundNumber != null && roundNumber <= upToRoundNumber;
            });
    }

    @Override
    public List<Long> getOpponentIds(Long traineeId, int upToRoundNumber) {
        List<MatchResult> results = getPlayerMatchResultsUpToRound(traineeId, upToRoundNumber);

        return results.stream()
            .map(result -> result.getWhiteTrainee().getId().equals(traineeId)
                ? result.getBlackTrainee().getId()
                : result.getWhiteTrainee().getId())
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public double getPointsForMatch(MatchResult result, Long traineeId) {
        boolean isWhite = result.getWhiteTrainee().getId().equals(traineeId);

        // Convert MatchResultType to points
        if (result.getResultType() == MatchResultType.DRAW) {
            return 0.5;
        } else if (result.getResultType() == MatchResultType.WHITE_WIN && isWhite) {
            return 1.0;
        } else if (result.getResultType() == MatchResultType.BLACK_WIN && !isWhite) {
            return 1.0;
        } else {
            return 0.0;
        }
    }

    /**
     * Helper: Extract swiss_round_number from a MatchResult by querying MatchParticipant.
     */
    private Integer getSwissRoundFromResult(MatchResult result, Long traineeId) {
        List<MatchParticipant> participants = matchParticipantRepository.findByMatchId(result.getMatch().getId());
        return participants.stream()
            .filter(p -> p.getTrainee().getId().equals(traineeId))
            .map(MatchParticipant::getSwissRoundNumber)
            .findFirst()
            .orElse(null);
    }
}
