// This service calculates Swiss tournament scores and tiebreakers.
package com.chesscoach.main.service;

import com.chesscoach.main.model.MatchResult;

import java.util.List;

public interface SwissScorer {

    /**
     * Calculate total score for a trainee up to and including a specific Swiss round.
     * Win = 1.0 point, Draw = 0.5 points, Loss = 0.0 points
     *
     * @param traineeId the trainee ID
     * @param upToRoundNumber the Swiss round number (inclusive)
     * @return total score as double
     */
    double getPlayerScore(Long traineeId, int upToRoundNumber);

    /**
     * Calculate Buchholz score (sum of all opponents' final scores) for a trainee.
     * Used as primary tiebreaker after wins.
     *
     * @param traineeId the trainee ID
     * @param upToRoundNumber the Swiss round number (inclusive)
     * @return sum of all opponents' final scores
     */
    double getPlayerBuchholzScore(Long traineeId, int upToRoundNumber);

    /**
     * Get all match results for a trainee up to a specific round.
     *
     * @param traineeId the trainee ID
     * @param upToRoundNumber the Swiss round number (inclusive)
     * @return list of MatchResult objects
     */
    List<MatchResult> getPlayerMatchResultsUpToRound(Long traineeId, int upToRoundNumber);

    /**
     * Check if two trainees have already played each other in a specific round.
     *
     * @param traineeId1 first trainee ID
     * @param traineeId2 second trainee ID
     * @param upToRoundNumber the Swiss round number (inclusive)
     * @return true if they played each other, false otherwise
     */
    boolean havePlayedEachOther(Long traineeId1, Long traineeId2, int upToRoundNumber);

    /**
     * Get list of opponents a trainee has faced up to a specific round.
     *
     * @param traineeId the trainee ID
     * @param upToRoundNumber the Swiss round number (inclusive)
     * @return list of opponent trainee IDs
     */
    List<Long> getOpponentIds(Long traineeId, int upToRoundNumber);

    /**
     * Calculate points earned for a specific match result (1.0, 0.5, or 0.0).
     *
     * @param result the MatchResult
     * @param traineeId the trainee ID (to determine if white or black)
     * @return points earned (1.0 for win, 0.5 for draw, 0.0 for loss)
     */
    double getPointsForMatch(MatchResult result, Long traineeId);
}
