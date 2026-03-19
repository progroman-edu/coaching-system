// This service implementation contains business logic for Rating operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.match.TraineeRatingHistoryResponse;
import com.chesscoach.main.model.MatchResult;
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

        int whiteOld = white.getCurrentRating();
        int blackOld = black.getCurrentRating();
        int whiteNew = EloRatingCalculator.calculateNewRating(whiteOld, blackOld, matchResult.getWhiteScore(), kFactor);
        int blackNew = EloRatingCalculator.calculateNewRating(blackOld, whiteOld, matchResult.getBlackScore(), kFactor);

        white.setCurrentRating(whiteNew);
        black.setCurrentRating(blackNew);

        // Update mode-specific highest ratings
        updateModeSpecificHighest(white, whiteNew, white.getCurrentRatingMode());
        updateModeSpecificHighest(black, blackNew, black.getCurrentRatingMode());

        traineeRepository.save(white);
        traineeRepository.save(black);

        ratingsHistoryRepository.save(buildRatingHistory(white, matchResult, whiteOld, whiteNew));
        ratingsHistoryRepository.save(buildRatingHistory(black, matchResult, blackOld, blackNew));

        recomputeRankings();
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
        List<Trainee> leaderboard = traineeRepository.findAllByOrderByCurrentRatingDescIdAsc();
        int rank = 1;
        for (Trainee trainee : leaderboard) {
            trainee.setRanking(rank++);
        }
        traineeRepository.saveAll(leaderboard);
    }

    private void updateModeSpecificHighest(Trainee trainee, int newRating, String mode) {
        if (mode == null) {
            return;
        }
        switch (mode.toLowerCase()) {
            case "rapid" -> trainee.setHighestRapidRating(
                    Math.max(newRating, safeValue(trainee.getHighestRapidRating()))
            );
            case "blitz" -> trainee.setHighestBlitzRating(
                    Math.max(newRating, safeValue(trainee.getHighestBlitzRating()))
            );
            case "bullet" -> trainee.setHighestBulletRating(
                    Math.max(newRating, safeValue(trainee.getHighestBulletRating()))
            );
        }
    }

    private int safeValue(Integer val) {
        return val != null ? val : 0;
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

    private int safeHighest(Trainee trainee) {
        return trainee.getHighestRating() != null ? trainee.getHighestRating() : trainee.getCurrentRating();
    }
}

