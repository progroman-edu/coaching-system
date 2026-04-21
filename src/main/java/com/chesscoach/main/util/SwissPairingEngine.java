// This utility generates Swiss pairings using score groups, repeat-avoidance, and rating proximity.
package com.chesscoach.main.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class SwissPairingEngine {

    private static final Comparator<PlayerContext> PLAYER_ORDER = Comparator
        .comparingDouble(PlayerContext::score).reversed()
        .thenComparingInt(PlayerContext::rating).reversed()
        .thenComparingLong(PlayerContext::traineeId);

    private SwissPairingEngine() {
    }

    public static List<SwissPairingGenerator.Pairing> generatePairings(List<PlayerContext> players) {
        if (players == null || players.isEmpty()) {
            return List.of();
        }

        List<PlayerContext> working = players.stream()
            .sorted(PLAYER_ORDER)
            .collect(Collectors.toCollection(ArrayList::new));

        List<SwissPairingGenerator.Pairing> pairings = new ArrayList<>();

        if (working.size() % 2 != 0) {
            PlayerContext byePlayer = chooseByePlayer(working);
            working.remove(byePlayer);
            pairings.add(new SwissPairingGenerator.Pairing(byePlayer.traineeId(), null));
        }

        Map<Double, List<PlayerContext>> groups = new TreeMap<>(Comparator.reverseOrder());
        for (PlayerContext player : working) {
            groups.computeIfAbsent(player.score(), key -> new ArrayList<>()).add(player);
        }

        List<PlayerContext> carryDown = new ArrayList<>();
        for (List<PlayerContext> group : groups.values()) {
            List<PlayerContext> pool = new ArrayList<>(carryDown);
            group.sort(PLAYER_ORDER);
            pool.addAll(group);
            pool.sort(PLAYER_ORDER);
            carryDown.clear();

            pairWithinPool(pool, carryDown, pairings);
        }

        if (!carryDown.isEmpty()) {
            pairCarryDown(carryDown, pairings);
        }

        return pairings;
    }

    private static PlayerContext chooseByePlayer(List<PlayerContext> players) {
        List<PlayerContext> neverHadBye = players.stream()
            .filter(player -> !player.hadBye())
            .toList();
        List<PlayerContext> targetPool = neverHadBye.isEmpty() ? players : neverHadBye;

        return targetPool.stream()
            .min(Comparator.comparingDouble(PlayerContext::score)
                .thenComparingInt(PlayerContext::rating)
                .thenComparingLong(PlayerContext::traineeId))
            .orElseThrow();
    }

    private static void pairWithinPool(
        List<PlayerContext> pool,
        List<PlayerContext> carryDown,
        List<SwissPairingGenerator.Pairing> pairings
    ) {
        while (pool.size() > 1) {
            PlayerContext current = pool.remove(0);
            PlayerContext opponent = selectOpponent(current, pool);
            if (opponent == null) {
                carryDown.add(current);
                continue;
            }
            pool.remove(opponent);
            pairings.add(asPairing(current, opponent));
        }
        if (pool.size() == 1) {
            carryDown.add(pool.remove(0));
        }
    }

    private static void pairCarryDown(List<PlayerContext> carryDown, List<SwissPairingGenerator.Pairing> pairings) {
        carryDown.sort(PLAYER_ORDER);
        while (carryDown.size() > 1) {
            PlayerContext current = carryDown.remove(0);
            PlayerContext opponent = carryDown.stream()
                .min(Comparator.comparingInt((PlayerContext candidate) -> Math.abs(current.rating() - candidate.rating()))
                    .thenComparingLong(PlayerContext::traineeId))
                .orElse(null);
            if (opponent == null) {
                break;
            }
            carryDown.remove(opponent);
            pairings.add(asPairing(current, opponent));
        }
    }

    private static PlayerContext selectOpponent(PlayerContext current, List<PlayerContext> candidates) {
        if (candidates.isEmpty()) {
            return null;
        }

        List<PlayerContext> sameScoreNoRepeat = candidates.stream()
            .filter(candidate -> Double.compare(candidate.score(), current.score()) == 0)
            .filter(candidate -> !current.opponentIds().contains(candidate.traineeId()))
            .sorted(Comparator.comparingInt((PlayerContext candidate) -> Math.abs(current.rating() - candidate.rating()))
                .thenComparingLong(PlayerContext::traineeId))
            .toList();
        if (!sameScoreNoRepeat.isEmpty()) {
            return sameScoreNoRepeat.get(0);
        }

        List<PlayerContext> sameScoreAny = candidates.stream()
            .filter(candidate -> Double.compare(candidate.score(), current.score()) == 0)
            .sorted(Comparator.comparingInt((PlayerContext candidate) -> Math.abs(current.rating() - candidate.rating()))
                .thenComparingLong(PlayerContext::traineeId))
            .toList();
        if (!sameScoreAny.isEmpty()) {
            return sameScoreAny.get(0);
        }

        List<PlayerContext> anyScoreNoRepeat = candidates.stream()
            .filter(candidate -> !current.opponentIds().contains(candidate.traineeId()))
            .sorted(Comparator.comparingDouble((PlayerContext candidate) -> Math.abs(current.score() - candidate.score()))
                .thenComparingInt((PlayerContext candidate) -> Math.abs(current.rating() - candidate.rating()))
                .thenComparingLong(PlayerContext::traineeId))
            .toList();
        if (!anyScoreNoRepeat.isEmpty()) {
            return anyScoreNoRepeat.get(0);
        }

        return candidates.stream()
            .min(Comparator.comparingDouble((PlayerContext candidate) -> Math.abs(current.score() - candidate.score()))
                .thenComparingInt((PlayerContext candidate) -> Math.abs(current.rating() - candidate.rating()))
                .thenComparingLong(PlayerContext::traineeId))
            .orElse(null);
    }

    private static SwissPairingGenerator.Pairing asPairing(PlayerContext a, PlayerContext b) {
        if (a.rating() > b.rating()) {
            return new SwissPairingGenerator.Pairing(a.traineeId(), b.traineeId());
        }
        if (b.rating() > a.rating()) {
            return new SwissPairingGenerator.Pairing(b.traineeId(), a.traineeId());
        }
        return a.traineeId() < b.traineeId()
            ? new SwissPairingGenerator.Pairing(a.traineeId(), b.traineeId())
            : new SwissPairingGenerator.Pairing(b.traineeId(), a.traineeId());
    }

    public record PlayerContext(
        Long traineeId,
        double score,
        int rating,
        List<Long> opponentIds,
        boolean hadBye
    ) {
    }
}
