// This service implements Swiss tournament orchestration.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.exception.ConflictException;
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
import com.chesscoach.main.repository.MatchResultRepository;
import com.chesscoach.main.repository.RematachRoundRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.SwissScorer;
import com.chesscoach.main.service.SwissTiebreaker;
import com.chesscoach.main.service.SwissTournamentService;
import com.chesscoach.main.util.SwissPairingEngine;
import com.chesscoach.main.util.SwissPairingGenerator;
import com.chesscoach.main.util.SwissPairingGenerator.Pairing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SwissTournamentServiceImpl implements SwissTournamentService {

    private static final Logger log = LoggerFactory.getLogger(SwissTournamentServiceImpl.class);

    private final MatchRepository matchRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchResultRepository matchResultRepository;
    private final TraineeRepository traineeRepository;
    private final RematachRoundRepository rematachRoundRepository;
    private final SwissScorer swissScorer;
    private final SwissTiebreaker swissTiebreaker;

    @Value("${app.rating.default:1200}")
    private int defaultRating;

    public SwissTournamentServiceImpl(
        MatchRepository matchRepository,
        MatchParticipantRepository matchParticipantRepository,
        MatchResultRepository matchResultRepository,
        TraineeRepository traineeRepository,
        RematachRoundRepository rematachRoundRepository,
        SwissScorer swissScorer,
        SwissTiebreaker swissTiebreaker
    ) {
        this.matchRepository = matchRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.matchResultRepository = matchResultRepository;
        this.traineeRepository = traineeRepository;
        this.rematachRoundRepository = rematachRoundRepository;
        this.swissScorer = swissScorer;
        this.swissTiebreaker = swissTiebreaker;
    }

    @Override
    @Transactional
    public List<Pairing> generateNextRound(int roundNumber) {
        log.info("Generating Swiss round {}", roundNumber);

        // Validate round number
        if (roundNumber <= 0) {
            throw new IllegalArgumentException("Round number must be >= 1");
        }

        Integer maxExistingRound = matchRepository.findMaxRoundNumberByFormat(MatchFormat.SWISS);
        if (maxExistingRound == null) {
            maxExistingRound = 0;
        }

        if (roundNumber != maxExistingRound + 1) {
            throw new ConflictException("Swiss rounds must be sequential. Expected round " + (maxExistingRound + 1) + " but got " + roundNumber);
        }

        List<Trainee> trainees = traineeRepository.findAllWithRatingsOrderedByRating();
        if (trainees.isEmpty()) {
            throw new ResourceNotFoundException("No trainees available for Swiss pairing");
        }

        List<SwissPairingEngine.PlayerContext> contexts = new ArrayList<>();
        for (Trainee trainee : trainees) {
            double score = swissScorer.getPlayerScore(trainee.getId(), roundNumber - 1);
            List<Long> opponents = swissScorer.getOpponentIds(trainee.getId(), roundNumber - 1);
            int rating = trainee.getRapidRating() != null && trainee.getRapidRating().getCurrentRating() != null
                ? trainee.getRapidRating().getCurrentRating()
                : defaultRating;
            boolean hadBye = matchParticipantRepository.existsByTraineeIdAndSwissRoundNumberIsNotNullAndByeTrue(trainee.getId());
            contexts.add(new SwissPairingEngine.PlayerContext(trainee.getId(), score, rating, opponents, hadBye));
        }

        List<Pairing> pairings = SwissPairingEngine.generatePairings(contexts);

        log.debug("Generated {} pairings for round {}", pairings.size(), roundNumber);
        return pairings;
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

    private MatchParticipant createParticipant(Match match, Long traineeId, int boardNumber) {
        Trainee trainee = traineeRepository.findById(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + traineeId));

        MatchParticipant participant = new MatchParticipant();
        participant.setMatch(match);
        participant.setTrainee(trainee);
        participant.setBoardNumber(boardNumber);
        participant.setBye(false);
        participant.setPointsEarned(0.0);
        participant.setStartRating(
            trainee.getRapidRating() != null && trainee.getRapidRating().getCurrentRating() != null
                ? trainee.getRapidRating().getCurrentRating()
                : defaultRating
        );
        participant.setPieceColor(boardNumber == 1 ? com.chesscoach.main.model.PieceColor.WHITE : com.chesscoach.main.model.PieceColor.BLACK);
        return participant;
    }
}
