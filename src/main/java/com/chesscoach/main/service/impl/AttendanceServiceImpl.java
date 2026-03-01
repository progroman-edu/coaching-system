package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.attendance.AttendanceRecordRequest;
import com.chesscoach.main.dto.attendance.AttendanceReportResponse;
import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.Attendance;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.AttendanceRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.AttendanceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final TraineeRepository traineeRepository;

    public AttendanceServiceImpl(AttendanceRepository attendanceRepository, TraineeRepository traineeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.traineeRepository = traineeRepository;
    }

    @Override
    @Transactional
    public AttendanceRecordRequest recordAttendance(AttendanceRecordRequest request) {
        Trainee trainee = traineeRepository.findById(request.getTraineeId())
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + request.getTraineeId()));

        Attendance attendance = attendanceRepository
            .findByTraineeIdAndAttendanceDate(request.getTraineeId(), request.getAttendanceDate())
            .orElseGet(Attendance::new);

        attendance.setTrainee(trainee);
        attendance.setAttendanceDate(request.getAttendanceDate());
        attendance.setPresent(request.getPresent());
        attendance.setRemarks(request.getRemarks());
        attendanceRepository.save(attendance);
        return request;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceReportResponse> getAttendanceReport(LocalDate startDate, LocalDate endDate, Long traineeId) {
        if (traineeId != null) {
            return List.of(buildReportForTrainee(startDate, endDate, traineeId));
        }

        List<Attendance> records = attendanceRepository.findByAttendanceDateBetweenOrderByAttendanceDateAsc(startDate, endDate);
        Map<Long, List<Attendance>> byTrainee = new LinkedHashMap<>();
        for (Attendance record : records) {
            byTrainee.computeIfAbsent(record.getTrainee().getId(), ignored -> new ArrayList<>()).add(record);
        }

        List<AttendanceReportResponse> reports = new ArrayList<>();
        for (Map.Entry<Long, List<Attendance>> entry : byTrainee.entrySet()) {
            reports.add(toReport(startDate, endDate, entry.getValue()));
        }
        return reports;
    }

    private AttendanceReportResponse buildReportForTrainee(LocalDate startDate, LocalDate endDate, Long traineeId) {
        traineeRepository.findById(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + traineeId));

        List<Attendance> records = attendanceRepository
            .findByTraineeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(traineeId, startDate, endDate);
        return toReport(startDate, endDate, records);
    }

    private AttendanceReportResponse toReport(LocalDate startDate, LocalDate endDate, List<Attendance> records) {
        AttendanceReportResponse report = new AttendanceReportResponse();
        report.setStartDate(startDate);
        report.setEndDate(endDate);

        if (records.isEmpty()) {
            report.setSessionsPresent(0);
            report.setTotalSessions(0);
            report.setAttendancePercentage(0.0);
            return report;
        }

        Attendance first = records.getFirst();
        long presentCount = records.stream().filter(a -> Boolean.TRUE.equals(a.getPresent())).count();
        int total = records.size();

        report.setTraineeId(first.getTrainee().getId());
        report.setTraineeName(first.getTrainee().getName());
        report.setSessionsPresent((int) presentCount);
        report.setTotalSessions(total);
        report.setAttendancePercentage(total == 0 ? 0.0 : (presentCount * 100.0) / total);
        return report;
    }
}
