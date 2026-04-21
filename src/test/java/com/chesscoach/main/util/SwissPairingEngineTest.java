package com.chesscoach.main.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SwissPairingEngineTest {

    @Test
    void generatePairingsAssignsByeToLowestEligiblePlayerWithoutPriorBye() {
        List<SwissPairingEngine.PlayerContext> players = List.of(
            new SwissPairingEngine.PlayerContext(1L, 2.0, 1800, List.of(), false),
            new SwissPairingEngine.PlayerContext(2L, 1.5, 1700, List.of(), false),
            new SwissPairingEngine.PlayerContext(3L, 1.0, 1600, List.of(), true),
            new SwissPairingEngine.PlayerContext(4L, 0.5, 1500, List.of(), false),
            new SwissPairingEngine.PlayerContext(5L, 0.0, 1400, List.of(), false)
        );

        List<SwissPairingGenerator.Pairing> pairings = SwissPairingEngine.generatePairings(players);

        assertThat(pairings).contains(new SwissPairingGenerator.Pairing(5L, null));
    }

    @Test
    void generatePairingsAvoidsRepeatOpponentsWhenAlternativeExists() {
        List<SwissPairingEngine.PlayerContext> players = List.of(
            new SwissPairingEngine.PlayerContext(1L, 2.0, 1800, List.of(2L), false),
            new SwissPairingEngine.PlayerContext(2L, 2.0, 1790, List.of(1L), false),
            new SwissPairingEngine.PlayerContext(3L, 2.0, 1780, List.of(), false),
            new SwissPairingEngine.PlayerContext(4L, 2.0, 1770, List.of(), false)
        );

        List<SwissPairingGenerator.Pairing> pairings = SwissPairingEngine.generatePairings(players);

        assertThat(pairings).doesNotContain(new SwissPairingGenerator.Pairing(1L, 2L));
        assertThat(pairings).doesNotContain(new SwissPairingGenerator.Pairing(2L, 1L));
        assertThat(pairings).hasSize(2);
    }

    @Test
    void generatePairingsFallsBackToClosestRatingWhenRepeatsAreUnavoidable() {
        List<SwissPairingEngine.PlayerContext> players = List.of(
            new SwissPairingEngine.PlayerContext(10L, 1.0, 2000, List.of(20L, 30L, 40L), false),
            new SwissPairingEngine.PlayerContext(20L, 1.0, 1980, List.of(10L, 30L, 40L), false),
            new SwissPairingEngine.PlayerContext(30L, 1.0, 1500, List.of(10L, 20L, 40L), false),
            new SwissPairingEngine.PlayerContext(40L, 1.0, 1490, List.of(10L, 20L, 30L), false)
        );

        List<SwissPairingGenerator.Pairing> pairings = SwissPairingEngine.generatePairings(players);

        assertThat(pairings).contains(new SwissPairingGenerator.Pairing(10L, 20L));
        assertThat(pairings).contains(new SwissPairingGenerator.Pairing(30L, 40L));
    }
}
