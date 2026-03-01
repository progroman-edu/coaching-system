package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.analytics.DashboardAnalyticsResponse;
import com.chesscoach.main.dto.analytics.RatingTrendPointResponse;
import com.chesscoach.main.dto.analytics.TraineePerformanceResponse;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.service.AnalyticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.ANALYTICS)
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> dashboard(HttpServletRequest request) {
        DashboardAnalyticsResponse data = analyticsService.getDashboard();
        return ResponseEntity.ok(ApiResponse.ok("Dashboard analytics", data, request.getRequestURI()));
    }

    @GetMapping("/performance/{traineeId}")
    public ResponseEntity<ApiResponse<TraineePerformanceResponse>> performance(
        @PathVariable Long traineeId,
        HttpServletRequest request
    ) {
        TraineePerformanceResponse data = analyticsService.getPerformance(traineeId);
        return ResponseEntity.ok(ApiResponse.ok("Trainee performance", data, request.getRequestURI()));
    }

    @GetMapping("/rating-trend/{traineeId}")
    public ResponseEntity<ApiResponse<List<RatingTrendPointResponse>>> ratingTrend(
        @PathVariable Long traineeId,
        HttpServletRequest request
    ) {
        List<RatingTrendPointResponse> data = analyticsService.getRatingTrend(traineeId);
        return ResponseEntity.ok(ApiResponse.ok("Trainee rating trend", data, request.getRequestURI()));
    }
}
