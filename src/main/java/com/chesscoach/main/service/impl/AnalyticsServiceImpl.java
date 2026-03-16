// This service implementation contains business logic for Analytics operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.analytics.DashboardAnalyticsResponse;
import com.chesscoach.main.dto.analytics.RatingTrendPointResponse;
import com.chesscoach.main.dto.analytics.TraineePerformanceResponse;
import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.Attendance;
import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.MatchResultType;
import com.chesscoach.main.model.RatingsHistory;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.AttendanceRepository;
import com.chesscoach.main.repository.MatchResultRepository;
import com.chesscoach.main.repository.RatingsHistoryRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private final TraineeRepository traineeRepository;
    private final AttendanceRepository attendanceRepository;
    private final MatchResultRepository matchResultRepository;
    private final RatingsHistoryRepository ratingsHistoryRepository;

    public AnalyticsServiceImpl(
        TraineeRepository traineeRepository,
        AttendanceRepository attendanceRepository,
        MatchResultRepository matchResultRepository,
        RatingsHistoryRepository ratingsHistoryRepository
    ) {
        this.traineeRepository = traineeRepository;
        this.attendanceRepository = attendanceRepository;
        this.matchResultRepository = matchResultRepository;
        this.ratingsHistoryRepository = ratingsHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getDashboard() {
        List<Trainee> trainees = traineeRepository.findAll();
        List<Attendance> attendance = attendanceRepository.findAll();

        double averageRating = trainees.isEmpty()
            ? 0.0
            : trainees.stream().mapToInt(Trainee::getCurrentRating).average().orElse(0.0);
        long presentCount = attendance.stream().filter(a -> Boolean.TRUE.equals(a.getPresent())).count();
        double attendancePercentage = attendance.isEmpty() ? 0.0 : (presentCount * 100.0) / attendance.size();

        DashboardAnalyticsResponse response = new DashboardAnalyticsResponse();
        response.setTotalTrainees(trainees.size());
        response.setAverageRating(averageRating);
        response.setAttendancePercentage(attendancePercentage);
        response.setMatchesPlayed((int) matchResultRepository.count());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TraineePerformanceResponse getPerformance(Long traineeId) {
        Trainee trainee = traineeRepository.findById(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + traineeId));

        List<MatchResult> results = matchResultRepository.findByWhiteTraineeIdOrBlackTraineeIdOrderByPlayedAtDesc(traineeId, traineeId);
        List<RatingsHistory> history = ratingsHistoryRepository.findByTraineeIdOrderByCreatedAtDesc(traineeId);
        List<Attendance> attendance = attendanceRepository.findByTraineeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            traineeId,
            LocalDate.now().minusYears(1),
            LocalDate.now()
        );

        int wins = 0;
        int draws = 0;
        int losses = 0;
        for (MatchResult result : results) {
            boolean isWhite = result.getWhiteTrainee().getId().equals(traineeId);
            if (result.getResultType() == MatchResultType.DRAW) {
                draws++;
            } else if (result.getResultType() == MatchResultType.WHITE_WIN) {
                if (isWhite) {
                    wins++;
                } else {
                    losses++;
                }
            } else if (result.getResultType() == MatchResultType.BLACK_WIN) {
                if (isWhite) {
                    losses++;
                } else {
                    wins++;
                }
            }
        }

        long presentCount = attendance.stream().filter(a -> Boolean.TRUE.equals(a.getPresent())).count();
        double attendancePercentage = attendance.isEmpty() ? 0.0 : (presentCount * 100.0) / attendance.size();
        int highest = history.isEmpty()
            ? (trainee.getHighestRating() != null ? trainee.getHighestRating() : trainee.getCurrentRating())
            : history.stream().mapToInt(RatingsHistory::getNewRating).max().orElse(trainee.getCurrentRating());

        TraineePerformanceResponse response = new TraineePerformanceResponse();
        response.setTraineeId(trainee.getId());
        response.setTraineeName(trainee.getName());
        response.setCurrentRating(trainee.getCurrentRating());
        response.setHighestRating(highest);
        response.setMatchesPlayed(results.size());
        response.setWins(wins);
        response.setDraws(draws);
        response.setLosses(losses);
        response.setAttendancePercentage(attendancePercentage);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingTrendPointResponse> getRatingTrend(Long traineeId) {
        traineeRepository.findById(traineeId)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + traineeId));

        return ratingsHistoryRepository.findByTraineeIdOrderByCreatedAtDesc(traineeId)
            .stream()
            .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
            .map(history -> {
                RatingTrendPointResponse point = new RatingTrendPointResponse();
                point.setTimestamp(history.getCreatedAt());
                point.setRating(history.getNewRating());
                point.setChange(history.getRatingChange());
                return point;
            })
            .toList();
    }
}

