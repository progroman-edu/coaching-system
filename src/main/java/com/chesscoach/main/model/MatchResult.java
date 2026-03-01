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

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "match_results",
    uniqueConstraints = {
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    public Trainee getWhiteTrainee() {
        return whiteTrainee;
    }

    public void setWhiteTrainee(Trainee whiteTrainee) {
        this.whiteTrainee = whiteTrainee;
    }

    public Trainee getBlackTrainee() {
        return blackTrainee;
    }

    public void setBlackTrainee(Trainee blackTrainee) {
        this.blackTrainee = blackTrainee;
    }

    public Double getWhiteScore() {
        return whiteScore;
    }

    public void setWhiteScore(Double whiteScore) {
        this.whiteScore = whiteScore;
    }

    public Double getBlackScore() {
        return blackScore;
    }

    public void setBlackScore(Double blackScore) {
        this.blackScore = blackScore;
    }

    public MatchResultType getResultType() {
        return resultType;
    }

    public void setResultType(MatchResultType resultType) {
        this.resultType = resultType;
    }

    public OffsetDateTime getPlayedAt() {
        return playedAt;
    }

    public void setPlayedAt(OffsetDateTime playedAt) {
        this.playedAt = playedAt;
    }
}
