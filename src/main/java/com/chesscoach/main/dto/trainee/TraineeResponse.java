// This DTO defines response payload fields for Trainee endpoints.
package com.chesscoach.main.dto.trainee;

import java.time.OffsetDateTime;

public class TraineeResponse {
    private Long id;
    private String name;
    private Integer age;
    private String address;
    private String gradeLevel;
    private String courseStrand;
    private Integer currentRating;
    private String currentRatingMode;
    private Integer highestRating;
    private Integer highestRapidRating;
    private Integer highestBlitzRating;
    private Integer highestBulletRating;
    private Integer latestRatingChange;
    private Double attendancePercentageLast30Days;
    private OffsetDateTime lastActivityAt;
    private Integer ranking;
    private String photoPath;
    private String chessUsername;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Integer getLatestRatingChange() {
        return latestRatingChange;
    }

    public void setLatestRatingChange(Integer latestRatingChange) {
        this.latestRatingChange = latestRatingChange;
    }

    public Double getAttendancePercentageLast30Days() {
        return attendancePercentageLast30Days;
    }

    public void setAttendancePercentageLast30Days(Double attendancePercentageLast30Days) {
        this.attendancePercentageLast30Days = attendancePercentageLast30Days;
    }

    public OffsetDateTime getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(OffsetDateTime lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
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
}
