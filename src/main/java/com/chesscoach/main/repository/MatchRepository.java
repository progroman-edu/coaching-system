// This repository provides database access methods for Match records.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.Match;
import com.chesscoach.main.model.MatchFormat;
import com.chesscoach.main.model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByFormatAndRoundNumberOrderByScheduledDateAsc(MatchFormat format, Integer roundNumber);

    List<Match> findByStatusAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(MatchStatus status, LocalDate fromDate);

    @Query("SELECT COALESCE(MAX(m.roundNumber), 0) FROM Match m WHERE m.format = ?1")
    Integer findMaxRoundNumberByFormat(MatchFormat format);

    @Query("""
        SELECT COALESCE(MAX(m.roundNumber), 0)
        FROM Match m
        WHERE m.format = :format
          AND EXISTS (
              SELECT 1
              FROM MatchParticipant included
              WHERE included.match = m
                AND included.trainee.id IN :traineeIds
          )
          AND NOT EXISTS (
              SELECT 1
              FROM MatchParticipant excluded
              WHERE excluded.match = m
                AND excluded.trainee.id NOT IN :traineeIds
          )
    """)
    Integer findMaxRoundNumberByFormatAndTraineeIds(
        @Param("format") MatchFormat format,
        @Param("traineeIds") Collection<Long> traineeIds
    );
}
