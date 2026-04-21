// This file generates Swiss-style pairings from ordered trainee identifiers.
package com.chesscoach.main.util;

import com.chesscoach.main.model.Trainee;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Swiss pairing algorithm implementation.
 * 
 * Algorithm:
 * 1. Group players by score (descending)
 * 2. Within each score group, sort by rating (descending)
 * 3. Pair top player with next unpaired player, avoiding repeats
 * 4. If repeat unavoidable, use next available player
 * 5. Handle odd number with bye (null opponent)
 * 
 * Tiebreaker cascade:
 * - Primary: Wins (most wins ranks first)
 * - Secondary: Buchholz score (sum of opponents' final scores)
 * - Tertiary: If same score + same Buchholz + played each other + drew → rematch
 */
public final class SwissPairingGenerator {

    private SwissPairingGenerator() {
    }

    /**
     * Generate Swiss pairings for a given round.
     * 
     * @param traineesWithScores map of trainee to current score
     * @param opponentHistoryMap map of trainee to list of opponent IDs they've faced
     * @param roundNumber current round number (for logging/tracking)
     * @return list of pairings (Pairing records)
     */
    public static List<Pairing> generateSwiss(
        Map<Trainee, Double> traineesWithScores,
        Map<Long, List<Long>> opponentHistoryMap,
        int roundNumber
    ) {
        if (traineesWithScores.isEmpty()) {
            return new ArrayList<>();
        }

        // Group trainees by score (descending)
        List<List<Trainee>> scoreGroups = groupByScore(traineesWithScores);

        List<Pairing> pairings = new ArrayList<>();
        List<Trainee> unpaired = new ArrayList<>();

        // Iterate through score groups and pair
        for (List<Trainee> scoreGroup : scoreGroups) {
            List<Trainee> groupUnpaired = new ArrayList<>(scoreGroup);

            while (!groupUnpaired.isEmpty()) {
                Trainee player1 = groupUnpaired.remove(0);

                if (groupUnpaired.isEmpty()) {
                    // Odd player in this group, move to unpaired
                    unpaired.add(player1);
                    break;
                }

                // Find best opponent for player1 within group, avoiding repeats
                Trainee player2 = findBestOpponent(
                    player1,
                    groupUnpaired,
                    opponentHistoryMap,
                    traineesWithScores
                );

                if (player2 != null) {
                    groupUnpaired.remove(player2);
                    pairings.add(new Pairing(player1.getId(), player2.getId()));
                } else {
                    // No suitable opponent in group, add player1 to unpaired for later
                    unpaired.add(player1);
                }
            }
        }

        // Pair remaining unpaired players
        while (unpaired.size() >= 2) {
            Trainee player1 = unpaired.remove(0);
            Trainee player2 = findBestOpponent(
                player1,
                unpaired,
                opponentHistoryMap,
                traineesWithScores
            );

            if (player2 != null) {
                unpaired.remove(player2);
                pairings.add(new Pairing(player1.getId(), player2.getId()));
            } else {
                unpaired.add(player1); // Return to end of list
            }
        }

        // Handle bye (odd number of players)
        if (unpaired.size() == 1) {
            Trainee byePlayer = unpaired.get(0);
            pairings.add(new Pairing(byePlayer.getId(), null)); // null = bye
        }

        return pairings;
    }

    /**
     * Group trainees by score (descending), then by rating within each score.
     */
    private static List<List<Trainee>> groupByScore(Map<Trainee, Double> traineesWithScores) {
        // Create map of score -> list of trainees
        Map<Double, List<Trainee>> scoreMap = new HashMap<>();

        for (Map.Entry<Trainee, Double> entry : traineesWithScores.entrySet()) {
            Double score = entry.getValue();
            scoreMap.computeIfAbsent(score, k -> new ArrayList<>()).add(entry.getKey());
        }

        // Sort each group by rating (descending)
        return scoreMap.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getKey(), a.getKey())) // Descending score
            .map(entry -> {
                List<Trainee> group = entry.getValue();
                group.sort((t1, t2) -> {
                    Integer rating1 = t1.getRapidRating() != null ? t1.getRapidRating().getCurrentRating() : 1200;
                    Integer rating2 = t2.getRapidRating() != null ? t2.getRapidRating().getCurrentRating() : 1200;
                    return Integer.compare(rating2, rating1); // Descending rating
                });
                return group;
            })
            .collect(Collectors.toList());
    }

    /**
     * Find best opponent for player1 from available list, avoiding repeats.
     * Strategy:
     * 1. Prefer opponents not yet faced
     * 2. If all faced, pick closest rating
     * 3. Prioritize rating proximity
     */
    private static Trainee findBestOpponent(
        Trainee player1,
        List<Trainee> candidates,
        Map<Long, List<Long>> opponentHistoryMap,
        Map<Trainee, Double> traineesWithScores
    ) {
        if (candidates.isEmpty()) {
            return null;
        }

        List<Long> player1OpponentHistory = opponentHistoryMap.getOrDefault(player1.getId(), new ArrayList<>());

        // Prefer opponents not yet faced
        Trainee neverFaced = candidates.stream()
            .filter(candidate -> !player1OpponentHistory.contains(candidate.getId()))
            .min(Comparator.comparingInt(t -> Math.abs(getRating(t) - getRating(player1))))
            .orElse(null);

        if (neverFaced != null) {
            return neverFaced;
        }

        // All candidates already faced - pick closest rating anyway (fallback)
        return candidates.stream()
            .min(Comparator.comparingInt(t -> Math.abs(getRating(t) - getRating(player1))))
            .orElse(null);
    }

    /**
     * Get rapid rating or default.
     */
    private static int getRating(Trainee trainee) {
        if (trainee.getRapidRating() != null && trainee.getRapidRating().getCurrentRating() != null) {
            return trainee.getRapidRating().getCurrentRating();
        }
        return 1200; // Default rating
    }

    public record Pairing(Long whiteTraineeId, Long blackTraineeId) {
    }
}

