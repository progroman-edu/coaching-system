// This repository provides database access methods for Match records.
package com.chesscoach.main.repository;

import com.chesscoach.main.model.Match;
import com.chesscoach.main.model.MatchFormat;
import com.chesscoach.main.model.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {

    List<Match> findByFormatAndRoundNumberOrderByScheduledDateAsc(MatchFormat format, Integer roundNumber);

    List<Match> findByStatusAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(MatchStatus status, LocalDate fromDate);
}

