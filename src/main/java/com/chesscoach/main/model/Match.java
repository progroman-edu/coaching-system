// This JPA entity maps domain data for Match.
package com.chesscoach.main.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(
    name = "matches",
    indexes = {
        @Index(name = "idx_match_scheduled_date", columnList = "scheduled_date"),
        @Index(name = "idx_match_format_round", columnList = "format,round_number"),
        @Index(name = "idx_match_status", columnList = "status")
    }
)
public class Match extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 30)
    private MatchFormat format;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private MatchStatus status;

    @Column(name = "notes", length = 255)
    private String notes;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<MatchParticipant> participants = new ArrayList<>();

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<MatchResult> results = new ArrayList<>();

}

