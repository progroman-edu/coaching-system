// This repository provides database access methods for Trainee records.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.Trainee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TraineeRepository extends JpaRepository<Trainee, Long> {

    @Query("""
        SELECT t
        FROM Trainee t
        LEFT JOIN t.rapidRating r
        WHERE (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:ratingMin IS NULL OR r.currentRating >= :ratingMin)
          AND (:department IS NULL OR LOWER(t.department) LIKE LOWER(CONCAT('%', :department, '%')))
    """)
    Page<Trainee> search(
        @Param("search") String search,
        @Param("ratingMin") Integer ratingMin,
        @Param("department") String department,
        Pageable pageable
    );

    Optional<Trainee> findByNameIgnoreCaseAndCoachId(String name, Long coachId);

    Optional<Trainee> findByChessUsernameIgnoreCase(String chessUsername);
}

