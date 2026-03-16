// This repository provides database access methods for Trainee records.
package com.chesscoach.main.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chesscoach.main.model.Trainee;

public interface TraineeRepository extends JpaRepository<Trainee, Long> {

    @Query("""
        SELECT t
        FROM Trainee t
        WHERE (:ratingMin IS NULL OR t.currentRating >= :ratingMin)
    """)
    Page<Trainee> search(
        @Param("ratingMin") Integer ratingMin,
        Pageable pageable
    );

    Optional<Trainee> findByNameIgnoreCaseAndCoachId(String name, Long coachId);

    List<Trainee> findAllByOrderByCurrentRatingDescIdAsc();

    Optional<Trainee> findByChessUsernameIgnoreCase(String chessUsername);
}

