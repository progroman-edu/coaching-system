package com.chesscoach.main;

import com.chesscoach.main.model.MatchStatus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MatchStatusStateTransitionTest {

    @Test
    void testValidScheduledToCompletedTransition() {
        assertTrue(MatchStatus.canTransitionTo(MatchStatus.SCHEDULED, MatchStatus.COMPLETED));
    }

    @Test
    void testValidScheduledToCancelledTransition() {
        assertTrue(MatchStatus.canTransitionTo(MatchStatus.SCHEDULED, MatchStatus.CANCELLED));
    }

    @Test
    void testInvalidCompletedTransition() {
        assertFalse(MatchStatus.canTransitionTo(MatchStatus.COMPLETED, MatchStatus.SCHEDULED));
        assertFalse(MatchStatus.canTransitionTo(MatchStatus.COMPLETED, MatchStatus.CANCELLED));
    }

    @Test
    void testInvalidCancelledTransition() {
        assertFalse(MatchStatus.canTransitionTo(MatchStatus.CANCELLED, MatchStatus.SCHEDULED));
        assertFalse(MatchStatus.canTransitionTo(MatchStatus.CANCELLED, MatchStatus.COMPLETED));
    }

    @Test
    void testNullTransition() {
        assertFalse(MatchStatus.canTransitionTo(null, MatchStatus.COMPLETED));
        assertFalse(MatchStatus.canTransitionTo(MatchStatus.SCHEDULED, null));
    }
}
