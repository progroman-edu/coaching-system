// This service handles Swiss tiebreaker logic and final rankings.
package com.chesscoach.main.service;

import com.chesscoach.main.model.Trainee;
import java.util.List;

public interface SwissTiebreaker {

    /**
     * Rank trainees by Swiss tiebreaker rules.
     * 
     * Tiebreaker cascade:
     * 1. Most wins (highest score)
     * 2. Buchholz score (sum of opponents' final scores)
     * 3. If still tied and they played each other + drew → flag for rematch
     *
     * @param trainees list of trainees to rank
     * @param roundNumber current round number
     * @return ranked list of TraineeRanking objects
     */
    List<TraineeRanking> rankTraineesByTiebreaker(List<Trainee> trainees, int roundNumber);

    /**
     * Check if two trainees should have a rematch (tiebreaker condition 3).
     * Returns true if:
     * - Same final score
     * - Same Buchholz score
     * - Have played each other before
     * - Their result was a draw
     *
     * @param trainee1 first trainee
     * @param trainee2 second trainee
     * @param roundNumber current round number
     * @return true if rematch should be created, false otherwise
     */
    boolean shouldCreateRematch(Trainee trainee1, Trainee trainee2, int roundNumber);

    /**
     * Ranking data class containing score, Buchholz, and trainee info.
     */
    class TraineeRanking implements Comparable<TraineeRanking> {
        public final Trainee trainee;
        public final double score;
        public final double buchholzScore;
        public final int rank;

        public TraineeRanking(Trainee trainee, double score, double buchholzScore, int rank) {
            this.trainee = trainee;
            this.score = score;
            this.buchholzScore = buchholzScore;
            this.rank = rank;
        }

        @Override
        public int compareTo(TraineeRanking other) {
            // Sort by score (descending)
            if (Double.compare(other.score, this.score) != 0) {
                return Double.compare(other.score, this.score);
            }
            // Then by Buchholz (descending)
            if (Double.compare(other.buchholzScore, this.buchholzScore) != 0) {
                return Double.compare(other.buchholzScore, this.buchholzScore);
            }
            // If still tied, maintain order (will be flagged for rematch if conditions met)
            return 0;
        }
    }
}
