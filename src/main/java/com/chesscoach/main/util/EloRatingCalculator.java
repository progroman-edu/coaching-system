// This file provides simplified ELO rating calculation utilities.
package com.chesscoach.main.util;

public final class EloRatingCalculator {

    private static final int DEFAULT_K = 20;

    private EloRatingCalculator() {
    }

    public static int calculateNewRating(int currentRating, int opponentRating, double actualScore) {
        return calculateNewRating(currentRating, opponentRating, actualScore, DEFAULT_K);
    }

    public static int calculateNewRating(int currentRating, int opponentRating, double actualScore, int kFactor) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentRating - currentRating) / 400.0));
        return (int) Math.round(currentRating + kFactor * (actualScore - expectedScore));
    }
}

