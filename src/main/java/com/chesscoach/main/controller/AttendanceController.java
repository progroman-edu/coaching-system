// This controller exposes HTTP endpoints for Attendance workflows.
package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.attendance.AttendanceRecordRequest;
import com.chesscoach.main.dto.attendance.AttendanceReportResponse;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping(ApiPaths.ATTENDANCE)
@Tag(name = "Attendance", description = "APIs for recording and reporting trainee attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping
    @Operation(summary = "Record attendance", description = "Record a trainee's attendance for a specific date")
    public ResponseEntity<ApiResponse<AttendanceRecordRequest>> recordAttendance(
        @Valid @RequestBody AttendanceRecordRequest requestBody,
        HttpServletRequest request
    ) {
        AttendanceRecordRequest data = attendanceService.recordAttendance(requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("Attendance recorded", data, request.getRequestURI()));
    }

    @GetMapping("/report")
    @Operation(summary = "Get attendance report", description = "Get attendance report for date range, optionally filtered by trainee")
    public ResponseEntity<ApiResponse<List<AttendanceReportResponse>>> getAttendanceReport(
        @RequestParam LocalDate startDate,
        @RequestParam LocalDate endDate,
        @RequestParam(required = false) Long traineeId,
        HttpServletRequest request
    ) {
        List<AttendanceReportResponse> data = attendanceService.getAttendanceReport(startDate, endDate, traineeId);
        return ResponseEntity.ok(ApiResponse.ok("Attendance report", data, request.getRequestURI()));
    }
}

