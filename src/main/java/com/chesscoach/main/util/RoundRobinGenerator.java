package com.chesscoach.main.util;

import java.util.ArrayList;
import java.util.List;

public final class RoundRobinGenerator {

    private RoundRobinGenerator() {
    }

    public static List<Pairing> generateForRound(List<Long> traineeIds, int roundNumber) {
        if (traineeIds.isEmpty()) {
            return List.of();
        }

        List<Long> players = new ArrayList<>(traineeIds);
        if (players.size() % 2 != 0) {
            players.add(null);
        }

        int rounds = players.size() - 1;
        int normalizedRound = ((roundNumber - 1) % rounds + rounds) % rounds;

        List<Long> rotated = new ArrayList<>(players);
        for (int i = 0; i < normalizedRound; i++) {
            rotateKeepingFirst(rotated);
        }

        List<Pairing> pairings = new ArrayList<>();
        int half = rotated.size() / 2;
        for (int i = 0; i < half; i++) {
            Long white = rotated.get(i);
            Long black = rotated.get(rotated.size() - 1 - i);
            pairings.add(new Pairing(white, black));
        }
        return pairings;
    }

    private static void rotateKeepingFirst(List<Long> list) {
        if (list.size() <= 2) {
            return;
        }
        Long last = list.remove(list.size() - 1);
        list.add(1, last);
    }

    public record Pairing(Long whiteTraineeId, Long blackTraineeId) {
    }
}
