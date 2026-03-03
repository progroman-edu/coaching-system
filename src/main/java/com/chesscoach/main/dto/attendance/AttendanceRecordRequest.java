// This file contains project logic for AttendanceRecordRequest.
package com.chesscoach.main.dto.attendance;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class AttendanceRecordRequest {

    @NotNull
    private Long traineeId;

    @NotNull
    private LocalDate attendanceDate;

    @NotNull
    private Boolean present;

    private String remarks;

    public Long getTraineeId() {
        return traineeId;
    }

    public void setTraineeId(Long traineeId) {
        this.traineeId = traineeId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}

