// This repository provides database access for RematachRound entities.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.RematachRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RematachRoundRepository extends JpaRepository<RematachRound, Long> {

    @Query("SELECT r FROM RematachRound r WHERE r.trainee1.id = ?1 OR r.trainee2.id = ?1")
    List<RematachRound> findByTraineeId(Long traineeId);

    @Query("SELECT r FROM RematachRound r WHERE r.status = ?1")
    List<RematachRound> findByStatus(RematachRound.RematachStatus status);

    @Query("SELECT r FROM RematachRound r WHERE r.originalMatch.id = ?1")
    Optional<RematachRound> findByOriginalMatchId(Long originalMatchId);

    @Query("SELECT r FROM RematachRound r WHERE r.rematachMatch.id = ?1")
    Optional<RematachRound> findByRematachMatchId(Long rematachMatchId);

    @Query("SELECT r FROM RematachRound r WHERE r.trainee1.id = ?1 AND r.trainee2.id = ?2 AND r.originalMatch.id = ?3")
    Optional<RematachRound> findByTraineePairAndOriginalMatch(Long traineeId1, Long traineeId2, Long originalMatchId);
}
