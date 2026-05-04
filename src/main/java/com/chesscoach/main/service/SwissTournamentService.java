// This service orchestrates Swiss tournament generation and result handling.
package com.chesscoach.main.service;

import com.chesscoach.main.util.SwissPairingGenerator.Pairing;
import java.util.List;

public interface SwissTournamentService {

    /**
     * Generate Swiss pairings for the next round.
     * 
     * @param traineeIds participant scope for the tournament; when empty, all active trainees are used
     * @param roundNumber the round number to generate (must be >= current max round + 1)
     * @return list of pairings with white/black assignments
     * @throws IllegalArgumentException if round number is invalid
     */
    List<Pairing> generateNextRound(List<Long> traineeIds, int roundNumber);

    /**
     * Finalize a round after all results are recorded.
     * 
     * Responsibilities:
     * 1. Calculate final scores and Buchholz
     * 2. Rank trainees by tiebreaker
     * 3. Identify and create rematch rounds where needed
     * 4. Store rankings
     *
     * @param roundNumber the round to finalize
     */
    void finalizeRound(int roundNumber);

    /**
     * Get current standings for a specific round.
     * 
     * @param roundNumber the round
     * @return ranked list of trainees with scores
     */
    List<StandingRow> getStandings(int roundNumber);

    /**
     * Calculate the maximum recommended rounds for a Swiss tournament.
     * Formula: ceil(log2(participantCount)) ensures enough rounds to differentiate players.
     * Minimum of 2 rounds recommended.
     * 
     * @param participantCount the number of participants
     * @return maximum recommended rounds
     */
    int calculateMaxRounds(int participantCount);

    /**
     * DTO for standings display.
     */
    class StandingRow {
        public final int rank;
        public final String traineeName;
        public final double score;
        public final double buchholzScore;
        public final int wins;
        public final int draws;
        public final int losses;

        public StandingRow(int rank, String traineeName, double score, double buchholzScore, 
                          int wins, int draws, int losses) {
            this.rank = rank;
            this.traineeName = traineeName;
            this.score = score;
            this.buchholzScore = buchholzScore;
            this.wins = wins;
            this.draws = draws;
            this.losses = losses;
        }
    }
}
