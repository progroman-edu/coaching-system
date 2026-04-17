// This enum constrains allowed values for MatchStatus and validates state transitions.
package com.chesscoach.main.model;

public enum MatchStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED;

    public static boolean canTransitionTo(MatchStatus from, MatchStatus to) {
        if (from == null || to == null) {
            return false;
        }
        // Valid transitions:
        // SCHEDULED -> COMPLETED, CANCELLED
        // COMPLETED -> (no further transitions allowed)
        // CANCELLED -> (no further transitions allowed)
        return switch (from) {
            case SCHEDULED -> to == COMPLETED || to == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }
}

