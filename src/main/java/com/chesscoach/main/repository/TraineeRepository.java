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
        LEFT JOIN t.rapidRating r
        WHERE t.deletedAt IS NULL
          AND (:search IS NULL OR LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:ratingMin IS NULL OR r.currentRating >= :ratingMin)
          AND (:department IS NULL OR LOWER(t.department) LIKE LOWER(CONCAT('%', :department, '%')))
    """)
    Page<Trainee> search(
        @Param("search") String search,
        @Param("ratingMin") Integer ratingMin,
        @Param("department") String department,
        Pageable pageable
    );

    /**
     * Find all trainees ordered by rapid rating (descending).
     * Uses JOIN FETCH to load rapid ratings in a single query (prevents N+1).
     * Excludes soft-deleted trainees.
     * This is used for ranking recomputation.
     *
     * @return list of all active trainees with ratings loaded, ordered by rating desc
     */
    @Query("""
        SELECT DISTINCT t
        FROM Trainee t
        LEFT JOIN FETCH t.rapidRating
        WHERE t.deletedAt IS NULL
        ORDER BY COALESCE(t.rapidRating.currentRating, 1200) DESC, t.id ASC
    """)
    List<Trainee> findAllWithRatingsOrderedByRating();

    @Query("""
        SELECT DISTINCT t
        FROM Trainee t
        LEFT JOIN FETCH t.rapidRating r
        WHERE t.deletedAt IS NULL
          AND t.id IN :ids
        ORDER BY COALESCE(r.currentRating, 1200) DESC, t.id ASC
    """)
    List<Trainee> findAllByIdInWithRatingsOrderedByRating(@Param("ids") List<Long> ids);

    Optional<Trainee> findByNameIgnoreCaseAndCoachId(String name, Long coachId);

    Optional<Trainee> findByChessUsernameIgnoreCase(String chessUsername);
}

