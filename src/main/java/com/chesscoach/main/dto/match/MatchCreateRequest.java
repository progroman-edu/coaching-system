package com.chesscoach.main.dto.match;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class MatchCreateRequest {

    @NotNull
    private LocalDate scheduledDate;

    @NotEmpty
    private List<Long> traineeIds;

    @NotNull
    private String format;

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public List<Long> getTraineeIds() {
        return traineeIds;
    }

    public void setTraineeIds(List<Long> traineeIds) {
        this.traineeIds = traineeIds;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }
}
