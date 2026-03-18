// This JPA entity maps domain data for RatingsHistory.
package com.chesscoach.main.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(
    name = "ratings_history",
    indexes = {
        @Index(name = "idx_ratings_history_trainee", columnList = "trainee_id"),
        @Index(name = "idx_ratings_history_match_result", columnList = "match_result_id")
    }
)
public class RatingsHistory extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_result_id")
    private MatchResult matchResult;

    @Column(name = "old_rating", nullable = false)
    private Integer oldRating;

    @Column(name = "new_rating", nullable = false)
    private Integer newRating;

    @Column(name = "rating_change", nullable = false)
    private Integer ratingChange;

    @Column(name = "notes", length = 255)
    private String notes;

}

