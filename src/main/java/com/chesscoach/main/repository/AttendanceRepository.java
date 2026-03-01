package com.chesscoach.main.repository;

import com.chesscoach.main.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByTraineeIdAndAttendanceDate(Long traineeId, LocalDate attendanceDate);

    List<Attendance> findByTraineeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
        Long traineeId,
        LocalDate startDate,
        LocalDate endDate
    );

    List<Attendance> findByAttendanceDateBetweenOrderByAttendanceDateAsc(LocalDate startDate, LocalDate endDate);
}
