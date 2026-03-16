// This JPA entity maps domain data for MatchParticipant.
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

@Entity
@Table(
    name = "match_participants",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_match_participant_match_trainee", columnNames = {"match_id", "trainee_id"})
    },
    indexes = {
        @Index(name = "idx_match_participants_match", columnList = "match_id"),
        @Index(name = "idx_match_participants_trainee", columnList = "trainee_id")
    }
)
public class MatchParticipant extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    @Enumerated(EnumType.STRING)
    @Column(name = "piece_color", nullable = false, length = 20)
    private PieceColor pieceColor = PieceColor.NONE;

    @Column(name = "board_number")
    private Integer boardNumber;

    @Column(name = "start_rating")
    private Integer startRating;

    @Column(name = "points_earned")
    private Double pointsEarned;

    @Column(name = "is_bye", nullable = false)
    private Boolean bye = Boolean.FALSE;

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

    public Trainee getTrainee() {
        return trainee;
    }

    public void setTrainee(Trainee trainee) {
        this.trainee = trainee;
    }

    public PieceColor getPieceColor() {
        return pieceColor;
    }

    public void setPieceColor(PieceColor pieceColor) {
        this.pieceColor = pieceColor;
    }

    public Integer getBoardNumber() {
        return boardNumber;
    }

    public void setBoardNumber(Integer boardNumber) {
        this.boardNumber = boardNumber;
    }

    public Integer getStartRating() {
        return startRating;
    }

    public void setStartRating(Integer startRating) {
        this.startRating = startRating;
    }

    public Double getPointsEarned() {
        return pointsEarned;
    }

    public void setPointsEarned(Double pointsEarned) {
        this.pointsEarned = pointsEarned;
    }

    public Boolean getBye() {
        return bye;
    }

    public void setBye(Boolean bye) {
        this.bye = bye;
    }
}

