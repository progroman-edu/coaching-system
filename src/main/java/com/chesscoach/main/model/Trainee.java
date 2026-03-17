// This JPA entity maps domain data for Trainee.
package com.chesscoach.main.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "trainees",
    indexes = {
        @Index(name = "idx_trainee_name", columnList = "name"),
        @Index(name = "idx_trainee_current_rating", columnList = "current_rating"),
        @Index(name = "idx_trainee_age", columnList = "age"),
        @Index(name = "idx_trainee_course_strand", columnList = "course_strand"),
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

    @Column(name = "course_strand", nullable = false, length = 100)
    private String courseStrand;

    @Column(name = "current_rating", nullable = false)
    private Integer currentRating;

    @Column(name = "current_rating_mode", length = 10)
    private String currentRatingMode;

    @Column(name = "highest_rating")
    private Integer highestRating;

    @Column(name = "highest_rapid_rating")
    private Integer highestRapidRating;

    @Column(name = "highest_blitz_rating")
    private Integer highestBlitzRating;

    @Column(name = "highest_bullet_rating")
    private Integer highestBulletRating;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Coach getCoach() {
        return coach;
    }

    public void setCoach(Coach coach) {
        this.coach = coach;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public String getCourseStrand() {
        return courseStrand;
    }

    public void setCourseStrand(String courseStrand) {
        this.courseStrand = courseStrand;
    }

    public Integer getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(Integer currentRating) {
        this.currentRating = currentRating;
    }

    public String getCurrentRatingMode() {
        return currentRatingMode;
    }

    public void setCurrentRatingMode(String currentRatingMode) {
        this.currentRatingMode = currentRatingMode;
    }

    public Integer getHighestRating() {
        return highestRating;
    }

    public void setHighestRating(Integer highestRating) {
        this.highestRating = highestRating;
    }

    public Integer getHighestRapidRating() {
        return highestRapidRating;
    }

    public void setHighestRapidRating(Integer highestRapidRating) {
        this.highestRapidRating = highestRapidRating;
    }

    public Integer getHighestBlitzRating() {
        return highestBlitzRating;
    }

    public void setHighestBlitzRating(Integer highestBlitzRating) {
        this.highestBlitzRating = highestBlitzRating;
    }

    public Integer getHighestBulletRating() {
        return highestBulletRating;
    }

    public void setHighestBulletRating(Integer highestBulletRating) {
        this.highestBulletRating = highestBulletRating;
    }

    public Integer getHighestRatingForMode(String mode) {
        if (mode == null) {
            return getCurrentRating();
        }
        return switch (mode.toLowerCase()) {
            case "rapid" -> highestRapidRating != null ? highestRapidRating : getCurrentRating();
            case "blitz" -> highestBlitzRating != null ? highestBlitzRating : getCurrentRating();
            case "bullet" -> highestBulletRating != null ? highestBulletRating : getCurrentRating();
            default -> getCurrentRating();
        };
    }

    public Integer getRanking() {
        return ranking;
    }

    public void setRanking(Integer ranking) {
        this.ranking = ranking;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getChessUsername() {
        return chessUsername;
    }

    public void setChessUsername(String chessUsername) {
        this.chessUsername = chessUsername;
    }

    public List<Attendance> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(List<Attendance> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public List<RatingsHistory> getRatingsHistory() {
        return ratingsHistory;
    }

    public void setRatingsHistory(List<RatingsHistory> ratingsHistory) {
        this.ratingsHistory = ratingsHistory;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
    }
}

