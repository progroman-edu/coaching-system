// This file contains project logic for Attendance.
package com.chesscoach.main.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(
    name = "attendance",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_attendance_trainee_date", columnNames = {"trainee_id", "attendance_date"})
    },
    indexes = {
        @Index(name = "idx_attendance_date", columnList = "attendance_date"),
        @Index(name = "idx_attendance_trainee", columnList = "trainee_id")
    }
)
public class Attendance extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trainee_id", nullable = false)
    private Trainee trainee;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "is_present", nullable = false)
    private Boolean present;

    @Column(name = "remarks", length = 255)
    private String remarks;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trainee getTrainee() {
        return trainee;
    }

    public void setTrainee(Trainee trainee) {
        this.trainee = trainee;
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

