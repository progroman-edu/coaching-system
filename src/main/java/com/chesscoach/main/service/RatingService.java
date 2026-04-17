// This service interface defines operations for Rating workflows.
package com.chesscoach.main.service;

import com.chesscoach.main.dto.match.TraineeRatingHistoryResponse;
import com.chesscoach.main.model.MatchResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RatingService {
    void applyMatchResultRatingUpdate(MatchResult matchResult);
    List<TraineeRatingHistoryResponse> getRatingHistoryByTrainee(Long traineeId);
    Page<TraineeRatingHistoryResponse> getRatingHistoryByTraineePaginated(Long traineeId, Pageable pageable);
}

