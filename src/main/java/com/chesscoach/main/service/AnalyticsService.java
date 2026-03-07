// This service interface defines operations for Analytics workflows.
package com.chesscoach.main.service;

import com.chesscoach.main.dto.analytics.DashboardAnalyticsResponse;
import com.chesscoach.main.dto.analytics.RatingTrendPointResponse;
import com.chesscoach.main.dto.analytics.TraineePerformanceResponse;

import java.util.List;

public interface AnalyticsService {
    DashboardAnalyticsResponse getDashboard();

    TraineePerformanceResponse getPerformance(Long traineeId);

    List<RatingTrendPointResponse> getRatingTrend(Long traineeId);
}

