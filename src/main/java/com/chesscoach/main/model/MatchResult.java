// This JPA entity maps domain data for MatchResult.
package com.chesscoach.main.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(
    name = "match_results",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_match_result_one_per_match", columnNames = {"match_id"}),
        @UniqueConstraint(name = "uk_match_result_match_players", columnNames = {"match_id", "white_trainee_id", "black_trainee_id"})
    },
    indexes = {
        @Index(name = "idx_match_results_match", columnList = "match_id"),
        @Index(name = "idx_match_results_white", columnList = "white_trainee_id"),
        @Index(name = "idx_match_results_black", columnList = "black_trainee_id"),
        @Index(name = "idx_match_results_played_at", columnList = "played_at")
    }
)
public class MatchResult extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "white_trainee_id", nullable = false)
    private Trainee whiteTrainee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "black_trainee_id", nullable = false)
    private Trainee blackTrainee;

    @Column(name = "white_score", nullable = false)
    private Double whiteScore;

    @Column(name = "black_score", nullable = false)
    private Double blackScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_type", nullable = false, length = 20)
    private MatchResultType resultType;

    @Column(name = "played_at", nullable = false)
    private OffsetDateTime playedAt;

}

