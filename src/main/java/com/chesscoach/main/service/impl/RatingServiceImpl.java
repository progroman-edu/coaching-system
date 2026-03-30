// This service implementation contains business logic for Rating operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.match.TraineeRatingHistoryResponse;
import com.chesscoach.main.model.BlitzRating;
import com.chesscoach.main.model.BulletRating;
import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.RapidRating;
import com.chesscoach.main.model.RatingsHistory;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.RatingsHistoryRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.RatingService;
import com.chesscoach.main.util.EloRatingCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RatingServiceImpl implements RatingService {

    private final TraineeRepository traineeRepository;
    private final RatingsHistoryRepository ratingsHistoryRepository;

    @Value("${app.rating.k-factor:20}")
    private int kFactor;

    public RatingServiceImpl(TraineeRepository traineeRepository, RatingsHistoryRepository ratingsHistoryRepository) {
        this.traineeRepository = traineeRepository;
        this.ratingsHistoryRepository = ratingsHistoryRepository;
    }

    @Override
    @Transactional
    public void applyMatchResultRatingUpdate(MatchResult matchResult) {
        Trainee white = matchResult.getWhiteTrainee();
        Trainee black = matchResult.getBlackTrainee();

        // Use rapid rating as the default for match calculations
        int whiteOld = getRapidCurrentRating(white);
        int blackOld = getRapidCurrentRating(black);
        int whiteNew = EloRatingCalculator.calculateNewRating(whiteOld, blackOld, matchResult.getWhiteScore(), kFactor);
        int blackNew = EloRatingCalculator.calculateNewRating(blackOld, whiteOld, matchResult.getBlackScore(), kFactor);

        // Update rapid ratings
        updateRapidRating(white, whiteNew);
        updateRapidRating(black, blackNew);

        traineeRepository.save(white);
        traineeRepository.save(black);

        ratingsHistoryRepository.save(buildRatingHistory(white, matchResult, whiteOld, whiteNew));
        ratingsHistoryRepository.save(buildRatingHistory(black, matchResult, blackOld, blackNew));

        recomputeRankings();
    }

    private int getRapidCurrentRating(Trainee trainee) {
        RapidRating rapid = trainee.getRapidRating();
        return rapid != null && rapid.getCurrentRating() != null ? rapid.getCurrentRating() : 1200;
    }

    private void updateRapidRating(Trainee trainee, int newRating) {
        RapidRating rapid = trainee.getRapidRating();
        if (rapid == null) {
            rapid = new RapidRating();
            rapid.setTrainee(trainee);
            trainee.setRapidRating(rapid);
        }
        rapid.setCurrentRating(newRating);
        if (rapid.getHighestRating() == null || newRating > rapid.getHighestRating()) {
            rapid.setHighestRating(newRating);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraineeRatingHistoryResponse> getRatingHistoryByTrainee(Long traineeId) {
        return ratingsHistoryRepository.findByTraineeIdOrderByCreatedAtDesc(traineeId)
            .stream()
            .map(this::toRatingHistoryResponse)
            .toList();
    }

    private void recomputeRankings() {
        List<Trainee> trainees = traineeRepository.findAll();
        trainees.sort((a, b) -> {
            int ratingA = getRapidCurrentRating(a);
            int ratingB = getRapidCurrentRating(b);
            if (ratingA != ratingB) {
                return Integer.compare(ratingB, ratingA); // descending
            }
            return Long.compare(a.getId(), b.getId()); // ascending by id
        });
        int rank = 1;
        for (Trainee trainee : trainees) {
            trainee.setRanking(rank++);
        }
        traineeRepository.saveAll(trainees);
    }

    private RatingsHistory buildRatingHistory(Trainee trainee, MatchResult result, int oldRating, int newRating) {
        RatingsHistory history = new RatingsHistory();
        history.setTrainee(trainee);
        history.setMatchResult(result);
        history.setOldRating(oldRating);
        history.setNewRating(newRating);
        history.setRatingChange(newRating - oldRating);
        history.setNotes("ELO update with K=" + kFactor + " from match result #" + result.getId());
        return history;
    }

    private TraineeRatingHistoryResponse toRatingHistoryResponse(RatingsHistory history) {
        TraineeRatingHistoryResponse response = new TraineeRatingHistoryResponse();
        response.setId(history.getId());
        response.setTraineeId(history.getTrainee().getId());
        response.setMatchHistoryId(history.getMatchResult() != null ? history.getMatchResult().getId() : null);
        response.setOldRating(history.getOldRating());
        response.setNewRating(history.getNewRating());
        response.setCreatedAt(history.getCreatedAt());
        response.setUpdatedAt(history.getUpdatedAt());
        return response;
    }
}

