// This file contains project logic for AttendanceReportResponse.
package com.chesscoach.main.dto.attendance;

import java.time.LocalDate;

public class AttendanceReportResponse {
    private Long traineeId;
    private String traineeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer sessionsPresent;
    private Integer totalSessions;
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

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getSessionsPresent() {
        return sessionsPresent;
    }

    public void setSessionsPresent(Integer sessionsPresent) {
        this.sessionsPresent = sessionsPresent;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(Double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }
}

