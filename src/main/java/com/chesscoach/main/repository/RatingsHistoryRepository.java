// This file contains project logic for RatingsHistoryRepository.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.RatingsHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingsHistoryRepository extends JpaRepository<RatingsHistory, Long> {

    List<RatingsHistory> findByTraineeIdOrderByCreatedAtDesc(Long traineeId);
}

