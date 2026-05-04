// This repository provides database access methods for MatchResult records.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    List<MatchResult> findByMatchIdOrderByPlayedAtDesc(Long matchId);

    List<MatchResult> findAllByOrderByPlayedAtAsc();

    List<MatchResult> findByWhiteTraineeIdOrBlackTraineeIdOrderByPlayedAtDesc(Long whiteTraineeId, Long blackTraineeId);

    List<MatchResult> findByWhiteTraineeIdAndBlackTraineeIdOrWhiteTraineeIdAndBlackTraineeId(Long whiteId1, Long blackId1, Long whiteId2, Long blackId2);
}

