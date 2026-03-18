// This DTO defines request payload fields for AttendanceRecord operations.
package com.chesscoach.main.dto.attendance;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class AttendanceRecordRequest {

    @NotNull
    private Long traineeId;

    @NotNull
    private LocalDate attendanceDate;

    @NotNull
    private Boolean present;

    private String remarks;

}

