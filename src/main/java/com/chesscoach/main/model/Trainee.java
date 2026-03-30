// This JPA entity maps domain data for Trainee.
package com.chesscoach.main.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@Entity
@Table(
    name = "trainees",
    indexes = {
        @Index(name = "idx_trainee_name", columnList = "name"),
        @Index(name = "idx_trainee_age", columnList = "age"),
        @Index(name = "idx_trainee_department", columnList = "department"),
        @Index(name = "idx_trainee_chess_username", columnList = "chess_username")
    }
)
public class Trainee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coach_id", nullable = false)
    private Coach coach;

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    @Column(name = "grade_level", nullable = false, length = 50)
    private String gradeLevel;

    @Column(name = "department", nullable = false, length = 100)
    private String department;

    @Column(name = "ranking")
    private Integer ranking;

    @Column(name = "photo_path", length = 255)
    private String photoPath;

    @Column(name = "chess_username", length = 80)
    private String chessUsername;

    @OneToMany(mappedBy = "trainee", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Attendance> attendanceRecords = new ArrayList<>();

    @OneToMany(mappedBy = "trainee", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<RatingsHistory> ratingsHistory = new ArrayList<>();

    @OneToMany(mappedBy = "trainee", cascade = CascadeType.ALL, orphanRemoval = false)
    private List<Notification> notifications = new ArrayList<>();

    @OneToOne(mappedBy = "trainee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BlitzRating blitzRating;

    @OneToOne(mappedBy = "trainee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private BulletRating bulletRating;

    @OneToOne(mappedBy = "trainee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private RapidRating rapidRating;

}

