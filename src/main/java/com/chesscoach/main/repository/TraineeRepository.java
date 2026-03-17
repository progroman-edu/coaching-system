// This repository provides database access methods for Trainee records.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.Trainee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TraineeRepository extends JpaRepository<Trainee, Long> {

    @Query("""
        SELECT t
        FROM Trainee t
        WHERE (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:ratingMin IS NULL OR t.currentRating >= :ratingMin)
          AND (:ratingMax IS NULL OR t.currentRating <= :ratingMax)
          AND (:ageMin IS NULL OR t.age >= :ageMin)
          AND (:ageMax IS NULL OR t.age <= :ageMax)
          AND (:courseStrand IS NULL OR LOWER(t.courseStrand) LIKE LOWER(CONCAT('%', :courseStrand, '%')))
          AND (:mode IS NULL OR LOWER(t.currentRatingMode) = LOWER(:mode))
    """)
    Page<Trainee> search(
        @Param("search") String search,
        @Param("ratingMin") Integer ratingMin,
        @Param("ratingMax") Integer ratingMax,
        @Param("ageMin") Integer ageMin,
        @Param("ageMax") Integer ageMax,
        @Param("courseStrand") String courseStrand,
        @Param("mode") String mode,
        Pageable pageable
    );

    Optional<Trainee> findByNameIgnoreCaseAndCoachId(String name, Long coachId);

    List<Trainee> findAllByOrderByCurrentRatingDescIdAsc();

    Optional<Trainee> findByChessUsernameIgnoreCase(String chessUsername);
}

