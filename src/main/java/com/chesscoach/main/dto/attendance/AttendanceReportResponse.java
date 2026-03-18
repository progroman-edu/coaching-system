// This DTO defines response payload fields for AttendanceReport endpoints.
package com.chesscoach.main.dto.attendance;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
public class AttendanceReportResponse {
    private Long traineeId;
    private String traineeName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer sessionsPresent;
    private Integer totalSessions;
    private Double attendancePercentage;

}

