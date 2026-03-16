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
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
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

}

