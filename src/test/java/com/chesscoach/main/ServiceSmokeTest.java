// This file verifies a basic service flow using the test profile.
package com.chesscoach.main;

import com.chesscoach.main.dto.analytics.DashboardAnalyticsResponse;
import com.chesscoach.main.dto.chesscom.ChessComRatingResponse;
import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;
import com.chesscoach.main.service.AnalyticsService;
import com.chesscoach.main.service.ChessComService;
import com.chesscoach.main.service.TraineeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ServiceSmokeTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        ChessComService chessComService() {
            return new ChessComService() {
                @Override
                public ChessComRatingResponse getRatings(String username) {
                    ChessComRatingResponse response = new ChessComRatingResponse();
                    response.setUsername(username);
                    response.setRapid(1200);
                    return response;
                }

                @Override
                public com.fasterxml.jackson.databind.JsonNode getArchives(String username) {
                    return null;
                }

                @Override
                public com.fasterxml.jackson.databind.JsonNode getMonthlyGames(String username, int year, int month) {
                    return null;
                }

                @Override
                public com.fasterxml.jackson.databind.JsonNode getAllModeHistory(String username, Integer limitArchives) {
                    return null;
                }

                @Override
                public com.chesscoach.main.dto.chesscom.ChessComSyncRatingResponse syncTraineeRating(Long traineeId, String mode) {
                    return null;
                }
            };
        }
    }

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
        request.setChessUsername("test-user");

        TraineeResponse created = traineeService.create(request);
        DashboardAnalyticsResponse dashboard = analyticsService.getDashboard();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Test Trainee");
        assertThat(dashboard.getTotalTrainees()).isGreaterThanOrEqualTo(1);
    }
}

