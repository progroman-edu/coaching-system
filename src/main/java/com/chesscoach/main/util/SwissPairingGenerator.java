package com.chesscoach.main.util;

import java.util.ArrayList;
import java.util.List;

public final class SwissPairingGenerator {

    private SwissPairingGenerator() {
    }

    public static List<Pairing> generate(List<Long> sortedTraineeIds) {
        List<Pairing> pairings = new ArrayList<>();
        for (int i = 0; i < sortedTraineeIds.size(); i += 2) {
            Long white = sortedTraineeIds.get(i);
            Long black = (i + 1 < sortedTraineeIds.size()) ? sortedTraineeIds.get(i + 1) : null;
            pairings.add(new Pairing(white, black));
        }
        return pairings;
    }

    public record Pairing(Long whiteTraineeId, Long blackTraineeId) {
    }
}
