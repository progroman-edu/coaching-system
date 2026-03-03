// This file verifies a basic service flow using the test profile.
package com.chesscoach.main;

import com.chesscoach.main.dto.analytics.DashboardAnalyticsResponse;
import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;
import com.chesscoach.main.service.AnalyticsService;
import com.chesscoach.main.service.TraineeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ServiceSmokeTest {

    @Autowired
    private TraineeService traineeService;

    @Autowired
    private AnalyticsService analyticsService;

    @Test
    void createTraineeAndReadDashboard() {
        TraineeRequest request = new TraineeRequest();
        request.setName("Test Trainee");
        request.setAge(16);
        request.setAddress("Test Address");
        request.setGradeLevel("10");
        request.setCourseStrand("STEM");
        request.setCurrentRating(1200);

        TraineeResponse created = traineeService.create(request);
        DashboardAnalyticsResponse dashboard = analyticsService.getDashboard();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Trainee");
        assertThat(dashboard.getTotalTrainees()).isGreaterThanOrEqualTo(1);
    }
}

