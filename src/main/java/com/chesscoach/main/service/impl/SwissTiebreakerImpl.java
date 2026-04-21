// This service implements Swiss tiebreaker logic and ranking.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.MatchResultType;
import com.chesscoach.main.model.RematachRound;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.RematachRoundRepository;
import com.chesscoach.main.service.SwissScorer;
import com.chesscoach.main.service.SwissTiebreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SwissTiebreakerImpl implements SwissTiebreaker {

    private static final Logger log = LoggerFactory.getLogger(SwissTiebreakerImpl.class);

    private final SwissScorer swissScorer;
    private final RematachRoundRepository rematachRoundRepository;

    public SwissTiebreakerImpl(SwissScorer swissScorer, RematachRoundRepository rematachRoundRepository) {
        this.swissScorer = swissScorer;
        this.rematachRoundRepository = rematachRoundRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraineeRanking> rankTraineesByTiebreaker(List<Trainee> trainees, int roundNumber) {
        log.debug("Ranking {} trainees by tiebreaker for round {}", trainees.size(), roundNumber);

        // Calculate scores and Buchholz for each trainee
        List<TraineeRanking> rankings = trainees.stream()
            .map(trainee -> {
                double score = swissScorer.getPlayerScore(trainee.getId(), roundNumber);
                double buchholz = swissScorer.getPlayerBuchholzScore(trainee.getId(), roundNumber);
                return new TraineeRanking(trainee, score, buchholz, 0); // Rank assigned later
            })
            .collect(Collectors.toList());

        // Sort by tiebreaker rules
        Collections.sort(rankings);

        // Assign ranks, accounting for ties
        int rank = 1;
        for (int i = 0; i < rankings.size(); i++) {
            TraineeRanking current = rankings.get(i);
            if (i > 0) {
                TraineeRanking previous = rankings.get(i - 1);
                if (Double.compare(current.score, previous.score) != 0 ||
                    Double.compare(current.buchholzScore, previous.buchholzScore) != 0) {
                    rank = i + 1;
                }
            }
            // Create new ranking object with assigned rank
            rankings.set(i, new TraineeRanking(current.trainee, current.score, current.buchholzScore, rank));
        }

        log.debug("Final rankings: {}", rankings.stream().map(r -> r.trainee.getName() + "=" + r.rank).collect(Collectors.joining(", ")));
        return rankings;
    }

    @Override
    @Transactional
    public boolean shouldCreateRematch(Trainee trainee1, Trainee trainee2, int roundNumber) {
        // Check if scores are identical
        double score1 = swissScorer.getPlayerScore(trainee1.getId(), roundNumber);
        double score2 = swissScorer.getPlayerScore(trainee2.getId(), roundNumber);

        if (Double.compare(score1, score2) != 0) {
            return false;
        }

        // Check if Buchholz scores are identical
        double buchholz1 = swissScorer.getPlayerBuchholzScore(trainee1.getId(), roundNumber);
        double buchholz2 = swissScorer.getPlayerBuchholzScore(trainee2.getId(), roundNumber);

        if (Double.compare(buchholz1, buchholz2) != 0) {
            return false;
        }

        // Check if they played each other before
        if (!swissScorer.havePlayedEachOther(trainee1.getId(), trainee2.getId(), roundNumber)) {
            return false;
        }

        // Check if their result was a draw
        List<MatchResult> results = swissScorer.getPlayerMatchResultsUpToRound(trainee1.getId(), roundNumber);
        boolean wasDraw = results.stream()
            .anyMatch(result -> {
                boolean sameMatchup = (result.getWhiteTrainee().getId().equals(trainee1.getId()) && 
                                       result.getBlackTrainee().getId().equals(trainee2.getId()))
                    || (result.getWhiteTrainee().getId().equals(trainee2.getId()) && 
                        result.getBlackTrainee().getId().equals(trainee1.getId()));
                return sameMatchup && result.getResultType() == MatchResultType.DRAW;
            });

        if (!wasDraw) {
            return false;
        }

        log.debug("Rematch condition met: {} vs {} (both score: {}, both Buchholz: {}, drew previously)", 
            trainee1.getName(), trainee2.getName(), score1, buchholz1);

        return true;
    }
}
