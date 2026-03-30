package com.chesscoach.main.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RoundRobinGeneratorTest {

    @Test
    void generateForRoundReturnsExpectedPairsForEvenPlayerCount() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L);

        List<RoundRobinGenerator.Pairing> round1 = RoundRobinGenerator.generateForRound(ids, 1);
        List<RoundRobinGenerator.Pairing> round2 = RoundRobinGenerator.generateForRound(ids, 2);
        List<RoundRobinGenerator.Pairing> round3 = RoundRobinGenerator.generateForRound(ids, 3);

        assertThat(round1).containsExactly(
            new RoundRobinGenerator.Pairing(1L, 4L),
            new RoundRobinGenerator.Pairing(2L, 3L)
        );
        assertThat(round2).containsExactly(
            new RoundRobinGenerator.Pairing(1L, 3L),
            new RoundRobinGenerator.Pairing(4L, 2L)
        );
        assertThat(round3).containsExactly(
            new RoundRobinGenerator.Pairing(1L, 2L),
            new RoundRobinGenerator.Pairing(3L, 4L)
        );
    }

    @Test
    void generateForRoundAddsByeForOddPlayerCount() {
        List<Long> ids = List.of(10L, 20L, 30L);

        List<RoundRobinGenerator.Pairing> pairings = RoundRobinGenerator.generateForRound(ids, 2);

        assertThat(pairings).containsExactly(
            new RoundRobinGenerator.Pairing(10L, 30L),
            new RoundRobinGenerator.Pairing(null, 20L)
        );
    }

    @Test
    void generateForRoundWrapsRoundNumber() {
        List<Long> ids = List.of(1L, 2L, 3L, 4L);

        List<RoundRobinGenerator.Pairing> round2 = RoundRobinGenerator.generateForRound(ids, 2);
        List<RoundRobinGenerator.Pairing> round5 = RoundRobinGenerator.generateForRound(ids, 5);

        assertThat(round5).containsExactlyElementsOf(round2);
    }
}
