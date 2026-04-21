// This repository provides database access methods for MatchParticipant records.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

    List<MatchParticipant> findByMatchIdOrderByBoardNumberAsc(Long matchId);

    List<MatchParticipant> findByTraineeIdOrderByCreatedAtDesc(Long traineeId);

    List<MatchParticipant> findByMatchId(Long matchId);

    boolean existsByTraineeIdAndSwissRoundNumberIsNotNullAndByeTrue(Long traineeId);
}

