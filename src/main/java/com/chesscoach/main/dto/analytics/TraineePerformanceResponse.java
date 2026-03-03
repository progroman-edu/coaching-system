// This file contains project logic for TraineePerformanceResponse.
package com.chesscoach.main.dto.analytics;

public class TraineePerformanceResponse {
    private Long traineeId;
    private String traineeName;
    private Integer currentRating;
    private Integer highestRating;
    private Integer matchesPlayed;
    private Integer wins;
    private Integer draws;
    private Integer losses;
    private Double attendancePercentage;

    public Long getTraineeId() {
        return traineeId;
    }

    public void setTraineeId(Long traineeId) {
        this.traineeId = traineeId;
    }

    public String getTraineeName() {
        return traineeName;
    }

    public void setTraineeName(String traineeName) {
        this.traineeName = traineeName;
    }

    public Integer getCurrentRating() {
        return currentRating;
    }

    public void setCurrentRating(Integer currentRating) {
        this.currentRating = currentRating;
    }

    public Integer getHighestRating() {
        return highestRating;
    }

    public void setHighestRating(Integer highestRating) {
        this.highestRating = highestRating;
    }

    public Integer getMatchesPlayed() {
        return matchesPlayed;
    }

    public void setMatchesPlayed(Integer matchesPlayed) {
        this.matchesPlayed = matchesPlayed;
    }

    public Integer getWins() {
        return wins;
    }

    public void setWins(Integer wins) {
        this.wins = wins;
    }

    public Integer getDraws() {
        return draws;
    }

    public void setDraws(Integer draws) {
        this.draws = draws;
    }

    public Integer getLosses() {
        return losses;
    }

    public void setLosses(Integer losses) {
        this.losses = losses;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}

