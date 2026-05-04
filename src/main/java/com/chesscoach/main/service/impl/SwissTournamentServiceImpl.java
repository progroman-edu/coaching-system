// This service implements Swiss tournament orchestration.
package com.chesscoach.main.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.Match;
import com.chesscoach.main.model.MatchFormat;
import com.chesscoach.main.model.MatchParticipant;
import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.MatchStatus;
import com.chesscoach.main.model.RematachRound;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.MatchParticipantRepository;
import com.chesscoach.main.repository.MatchRepository;
import com.chesscoach.main.repository.RematachRoundRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.SwissScorer;
import com.chesscoach.main.service.SwissTiebreaker;
import com.chesscoach.main.service.SwissTournamentService;
import com.chesscoach.main.util.SwissPairingEngine;
import com.chesscoach.main.util.SwissPairingGenerator.Pairing;

@Service
public class SwissTournamentServiceImpl implements SwissTournamentService {

    private static final Logger log = LoggerFactory.getLogger(SwissTournamentServiceImpl.class);

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final TraineeRepository traineeRepository;
    private final RematachRoundRepository rematachRoundRepository;
    private final SwissScorer swissScorer;
    private final SwissTiebreaker swissTiebreaker;

    @Value("${app.rating.default:1200}")
    private int defaultRating;

    public SwissTournamentServiceImpl(
        MatchRepository matchRepository,
        MatchParticipantRepository matchParticipantRepository,
        TraineeRepository traineeRepository,
        RematachRoundRepository rematachRoundRepository,
        SwissScorer swissScorer,
        SwissTiebreaker swissTiebreaker
    ) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.traineeRepository = traineeRepository;
        this.rematachRoundRepository = rematachRoundRepository;
        this.swissScorer = swissScorer;
        this.swissTiebreaker = swissTiebreaker;
    }

    @Override
    @Transactional
    @SuppressWarnings("UnboxingNullableValue")
    public List<Pairing> generateNextRound(List<Long> traineeIds, int roundNumber) {
        log.info("Generating Swiss round {}", roundNumber);

        // Validate round number
        if (roundNumber <= 0) {
            throw new IllegalArgumentException("Round number must be >= 1");
        }

        List<Long> scopedIds = normalizeTraineeIds(traineeIds);
        Integer maxExistingRound = scopedIds.isEmpty()
            ? matchRepository.findMaxRoundNumberByFormat(MatchFormat.SWISS)
            : matchRepository.findMaxRoundNumberByFormatAndTraineeIds(MatchFormat.SWISS, scopedIds);
        if (maxExistingRound == null) {
            maxExistingRound = 0;
        }

        int expectedRound = maxExistingRound + 1;
        if (roundNumber != expectedRound) {
            log.warn("Adjusting Swiss round request from {} to {}", roundNumber, expectedRound);
            roundNumber = expectedRound;
        }

        List<Trainee> trainees = scopedIds.isEmpty()
            ? traineeRepository.findAllWithRatingsOrderedByRating()
            : traineeRepository.findAllByIdInWithRatingsOrderedByRating(scopedIds);
        if (!scopedIds.isEmpty() && trainees.size() != scopedIds.size()) {
            throw new ResourceNotFoundException("One or more trainees not found");
        }
        if (trainees.isEmpty()) {
            throw new ResourceNotFoundException("No trainees available for Swiss pairing");
        }

        Set<Long> participantPool = trainees.stream()
            .map(Trainee::getId)
            .collect(Collectors.toSet());

        List<SwissPairingEngine.PlayerContext> contexts = new ArrayList<>();
        for (Trainee trainee : trainees) {
            List<MatchResult> priorResults = swissScorer.getPlayerMatchResultsUpToRound(trainee.getId(), roundNumber - 1).stream()
                .filter(result -> participantPool.contains(result.getWhiteTrainee().getId()))
                .filter(result -> participantPool.contains(result.getBlackTrainee().getId()))
                .toList();
            double score = priorResults.stream()
                .mapToDouble(result -> swissScorer.getPointsForMatch(result, trainee.getId()))
                .sum();
            List<Long> opponents = priorResults.stream()
                .map(result -> result.getWhiteTrainee().getId().equals(trainee.getId())
                    ? result.getBlackTrainee().getId()
                    : result.getWhiteTrainee().getId())
                .distinct()
                .toList();
            Integer currentRating = trainee.getRapidRating() != null && trainee.getRapidRating().getCurrentRating() != null
                ? trainee.getRapidRating().getCurrentRating()
                : defaultRating;
            int rating = currentRating != null ? currentRating : defaultRating;
            boolean hadBye = matchParticipantRepository.existsByTraineeIdAndSwissRoundNumberIsNotNullAndByeTrue(trainee.getId());
            contexts.add(new SwissPairingEngine.PlayerContext(trainee.getId(), score, rating, opponents, hadBye));
        }

        List<Pairing> pairings = SwissPairingEngine.generatePairings(contexts);

        log.debug("Generated {} pairings for round {}", pairings.size(), roundNumber);
        return pairings;
    }

    private List<Long> normalizeTraineeIds(List<Long> traineeIds) {
        if (traineeIds == null || traineeIds.isEmpty()) {
            return List.of();
        }
        return traineeIds.stream()
            .filter(id -> id != null && id > 0)
            .collect(Collectors.collectingAndThen(
                Collectors.toCollection(LinkedHashSet::new),
                ArrayList::new
            ));
    }

    @Override
    @Transactional
    public void finalizeRound(int roundNumber) {
        log.info("Finalizing Swiss round {}", roundNumber);

        // Get all trainees
        List<Trainee> trainees = traineeRepository.findAll();

        // Rank by tiebreaker
        List<SwissTiebreaker.TraineeRanking> rankings = swissTiebreaker.rankTraineesByTiebreaker(trainees, roundNumber);

        // Check for rematch conditions
        for (int i = 0; i < rankings.size(); i++) {
            for (int j = i + 1; j < rankings.size(); j++) {
                SwissTiebreaker.TraineeRanking r1 = rankings.get(i);
                SwissTiebreaker.TraineeRanking r2 = rankings.get(j);

                // Check if same rank (tied)
                if (r1.rank == r2.rank && swissTiebreaker.shouldCreateRematch(r1.trainee, r2.trainee, roundNumber)) {
                    createRematachRecord(r1.trainee, r2.trainee, roundNumber);
                }
            }
        }

        log.debug("Round {} finalized with {} trainees", roundNumber, trainees.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StandingRow> getStandings(int roundNumber) {
        List<Trainee> trainees = traineeRepository.findAll();
        List<SwissTiebreaker.TraineeRanking> rankings = swissTiebreaker.rankTraineesByTiebreaker(trainees, roundNumber);

        return rankings.stream()
            .map(ranking -> {
                List<MatchResult> results = swissScorer.getPlayerMatchResultsUpToRound(ranking.trainee.getId(), roundNumber);
                int wins = (int) results.stream().filter(r -> {
                    boolean isWhite = r.getWhiteTrainee().getId().equals(ranking.trainee.getId());
                    return (r.getResultType().name().contains("WHITE") && isWhite) ||
                           (r.getResultType().name().contains("BLACK") && !isWhite);
                }).count();
                int draws = (int) results.stream().filter(r -> r.getResultType().name().contains("DRAW")).count();
                int losses = results.size() - wins - draws;

                return new StandingRow(
                    ranking.rank,
                    ranking.trainee.getName(),
                    ranking.score,
                    ranking.buchholzScore,
                    wins,
                    draws,
                    losses
                );
            })
            .collect(Collectors.toList());
    }

    /**
     * Create a rematch round record for tiebreaker condition 3.
     */
    private void createRematachRecord(Trainee trainee1, Trainee trainee2, int roundNumber) {
        log.debug("Creating rematch for {} vs {} (round {} tiebreaker)", 
            trainee1.getName(), trainee2.getName(), roundNumber);

        // Find original match where they drew
        List<MatchResult> results = swissScorer.getPlayerMatchResultsUpToRound(trainee1.getId(), roundNumber);
        MatchResult originalDraw = results.stream()
            .filter(r -> {
                boolean sameMatchup = (r.getWhiteTrainee().getId().equals(trainee1.getId()) && 
                                       r.getBlackTrainee().getId().equals(trainee2.getId()))
                    || (r.getWhiteTrainee().getId().equals(trainee2.getId()) && 
                        r.getBlackTrainee().getId().equals(trainee1.getId()));
                return sameMatchup && r.getResultType().name().contains("DRAW");
            })
            .findFirst()
            .orElse(null);

        if (originalDraw == null) {
            return;
        }

        Trainee first = trainee1.getId() <= trainee2.getId() ? trainee1 : trainee2;
        Trainee second = trainee1.getId() <= trainee2.getId() ? trainee2 : trainee1;

        if (rematachRoundRepository.findByTraineePairAndOriginalMatch(first.getId(), second.getId(), originalDraw.getMatch().getId()).isPresent()) {
            return;
        }

        Match rematchMatch = createTiebreakerRematchMatch(first.getId(), second.getId(), roundNumber);

        RematachRound rematch = new RematachRound();
        rematch.setTrainee1(first);
        rematch.setTrainee2(second);
        rematch.setOriginalMatch(originalDraw.getMatch());
        rematch.setRematachMatch(rematchMatch);
        rematch.setReason("Tiebreaker rematch: same score and Buchholz after drawn direct encounter");
        rematch.setStatus(RematachRound.RematachStatus.SCHEDULED);
        rematachRoundRepository.save(rematch);
    }

    private Match createTiebreakerRematchMatch(Long whiteId, Long blackId, int roundNumber) {
        Match match = new Match();
        match.setFormat(MatchFormat.SWISS);
        match.setRoundNumber(roundNumber);
        match.setScheduledDate(LocalDate.now());
        match.setStatus(MatchStatus.SCHEDULED);
        match.setNotes("TIEBREAKER_REMATCH");
        Match savedMatch = matchRepository.save(match);

        MatchParticipant whiteParticipant = createParticipant(savedMatch, whiteId, 1);
        MatchParticipant blackParticipant = createParticipant(savedMatch, blackId, 2);
        matchParticipantRepository.save(whiteParticipant);
        matchParticipantRepository.save(blackParticipant);
        return savedMatch;
    }

    @SuppressWarnings("UnboxingNullableValue")
    private MatchParticipant createParticipant(Match match, Long traineeId, int boardNumber) {
        Trainee trainee = traineeRepository.findById(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + traineeId));

        MatchParticipant participant = new MatchParticipant();
        participant.setMatch(match);
        participant.setTrainee(trainee);
        participant.setBoardNumber(boardNumber);
        participant.setBye(false);
        participant.setPointsEarned(0.0);
        Integer currentRating = trainee.getRapidRating() != null && trainee.getRapidRating().getCurrentRating() != null
            ? trainee.getRapidRating().getCurrentRating()
            : defaultRating;
        participant.setStartRating(
            currentRating != null ? currentRating : defaultRating
        );
        participant.setPieceColor(boardNumber == 1 ? com.chesscoach.main.model.PieceColor.WHITE : com.chesscoach.main.model.PieceColor.BLACK);
        return participant;
    }

    @Override
    public int calculateMaxRounds(int participantCount) {
        if (participantCount <= 1) {
            return 1;
        }
        // Formula: ceil(log2(participantCount)) with minimum of 2 rounds
        int maxRounds = (int) Math.ceil(Math.log(participantCount) / Math.log(2));
        return Math.max(maxRounds, 2);
    }
}
