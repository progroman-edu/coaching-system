// This file contains project logic for MatchParticipantRepository.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, Long> {

    List<MatchParticipant> findByMatchIdOrderByBoardNumberAsc(Long matchId);

    List<MatchParticipant> findByTraineeIdOrderByCreatedAtDesc(Long traineeId);
}

