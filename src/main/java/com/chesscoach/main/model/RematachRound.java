// This JPA entity represents a tiebreaker rematch round in Swiss tournaments.
package com.chesscoach.main.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "rematch_round")
public class RematachRound extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trainee_id_1", nullable = false)
    private Trainee trainee1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trainee_id_2", nullable = false)
    private Trainee trainee2;

    @ManyToOne(optional = false)
    @JoinColumn(name = "original_match_id", nullable = false)
    private Match originalMatch;

    @ManyToOne(optional = true)
    @JoinColumn(name = "rematch_match_id", nullable = true)
    private Match rematachMatch;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RematachStatus status;

    public enum RematachStatus {
        PENDING,
        SCHEDULED,
        COMPLETED
    }
}
