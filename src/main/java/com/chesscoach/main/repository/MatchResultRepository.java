package com.chesscoach.main.repository;

import com.chesscoach.main.model.MatchResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchResultRepository extends JpaRepository<MatchResult, Long> {

    List<MatchResult> findByMatchIdOrderByPlayedAtDesc(Long matchId);

    List<MatchResult> findByWhiteTraineeIdOrBlackTraineeIdOrderByPlayedAtDesc(Long whiteTraineeId, Long blackTraineeId);
}
