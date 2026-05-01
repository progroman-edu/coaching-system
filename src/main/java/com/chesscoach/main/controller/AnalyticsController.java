// This controller exposes HTTP endpoints for Analytics workflows.
package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.analytics.DashboardAnalyticsResponse;
import com.chesscoach.main.dto.analytics.RatingTrendPointResponse;
import com.chesscoach.main.dto.analytics.TraineePerformanceResponse;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.ANALYTICS)
@Tag(name = "Analytics", description = "APIs for trainee performance analytics and rating trends")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard analytics", description = "Retrieve overall statistics including trainee count, average rating, attendance percentage, and match count")
    public ResponseEntity<ApiResponse<DashboardAnalyticsResponse>> dashboard(HttpServletRequest request) {
        DashboardAnalyticsResponse data = analyticsService.getDashboard();
        return ResponseEntity.ok(ApiResponse.ok("Dashboard analytics", data, request.getRequestURI()));
    }

    @GetMapping("/performance/{traineeId}")
    @Operation(summary = "Get trainee performance", description = "Retrieve match record, ratings, and attendance summary for a trainee")
    public ResponseEntity<ApiResponse<TraineePerformanceResponse>> performance(
        @PathVariable Long traineeId,
        HttpServletRequest request
    ) {
        TraineePerformanceResponse data = analyticsService.getPerformance(traineeId);
        return ResponseEntity.ok(ApiResponse.ok("Trainee performance analytics", data, request.getRequestURI()));
    }

    @GetMapping("/rating-trend/{traineeId}")
    @Operation(summary = "Get rating trend", description = "Retrieve rating change history for a trainee over time")
    public ResponseEntity<ApiResponse<List<RatingTrendPointResponse>>> ratingTrend(
        @PathVariable Long traineeId,
        HttpServletRequest request
    ) {
        List<RatingTrendPointResponse> data = analyticsService.getRatingTrend(traineeId);
        return ResponseEntity.ok(ApiResponse.ok("Trainee rating trend", data, request.getRequestURI()));
    }
}

