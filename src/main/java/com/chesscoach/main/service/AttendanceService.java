// This service interface defines operations for Attendance workflows.
package com.chesscoach.main.service;

import com.chesscoach.main.dto.attendance.AttendanceRecordRequest;
import com.chesscoach.main.dto.attendance.AttendanceReportResponse;

import java.time.LocalDate;
import java.util.List;

public interface AttendanceService {
    AttendanceRecordRequest recordAttendance(AttendanceRecordRequest request);

    List<AttendanceReportResponse> getAttendanceReport(LocalDate startDate, LocalDate endDate, Long traineeId);
}

