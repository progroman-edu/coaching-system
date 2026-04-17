// This service implementation contains business logic for Analytics operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.analytics.DashboardAnalyticsResponse;
import com.chesscoach.main.dto.analytics.RatingTrendPointResponse;
import com.chesscoach.main.dto.analytics.TraineePerformanceResponse;
import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.Attendance;
import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.MatchResultType;
import com.chesscoach.main.model.RapidRating;
import com.chesscoach.main.model.RatingsHistory;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.AttendanceRepository;
import com.chesscoach.main.repository.MatchResultRepository;
import com.chesscoach.main.repository.RatingsHistoryRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.AnalyticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsServiceImpl.class);

    private final TraineeRepository traineeRepository;
    private final AttendanceRepository attendanceRepository;
    private final MatchResultRepository matchResultRepository;
    private final RatingsHistoryRepository ratingsHistoryRepository;

    @Value("${app.rating.default:1200}")
    private int defaultRating;

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

    private int getRapidCurrentRating(Trainee trainee) {
        RapidRating rapid = trainee.getRapidRating();
        return rapid != null && rapid.getCurrentRating() != null ? rapid.getCurrentRating() : defaultRating;
    }

    private int getRapidHighestRating(Trainee trainee) {
        RapidRating rapid = trainee.getRapidRating();
        return rapid != null && rapid.getHighestRating() != null ? rapid.getHighestRating() : defaultRating;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getDashboard() {
        log.info("Generating dashboard analytics");
        
        List<Trainee> trainees = traineeRepository.findAll();
        List<Attendance> attendance = attendanceRepository.findAll();

        double averageRating = trainees.isEmpty()
            ? 0.0
            : trainees.stream().mapToInt(this::getRapidCurrentRating).average().orElse(0.0);
        long presentCount = attendance.stream().filter(a -> Boolean.TRUE.equals(a.getPresent())).count();
        double attendancePercentage = attendance.isEmpty() ? 0.0 : (presentCount * 100.0) / attendance.size();
        long matchesPlayed = matchResultRepository.count();

        log.debug("Dashboard metrics: trainees={}, avgRating={}, attendance={}%, matches={}", 
            trainees.size(), averageRating, attendancePercentage, matchesPlayed);

        DashboardAnalyticsResponse response = new DashboardAnalyticsResponse();
        response.setTotalTrainees(trainees.size());
        response.setAverageRating(averageRating);
        response.setAttendancePercentage(attendancePercentage);
        response.setMatchesPlayed((int) matchesPlayed);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TraineePerformanceResponse getPerformance(Long traineeId) {
        log.info("Generating performance analytics for trainee: {}", traineeId);
        
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
        int currentRating = getRapidCurrentRating(trainee);
        int highest = history.isEmpty()
            ? getRapidHighestRating(trainee)
            : history.stream().mapToInt(RatingsHistory::getNewRating).max().orElse(currentRating);

        log.debug("Performance metrics for trainee {}: rating={}, record={}-{}-{}, attendance={}%", 
            traineeId, currentRating, wins, losses, draws, attendancePercentage);

        TraineePerformanceResponse response = new TraineePerformanceResponse();
        response.setTraineeId(trainee.getId());
        response.setTraineeName(trainee.getName());
        response.setCurrentRating(currentRating);
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
        log.debug("Retrieving rating trend for trainee: {}", traineeId);
        
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

