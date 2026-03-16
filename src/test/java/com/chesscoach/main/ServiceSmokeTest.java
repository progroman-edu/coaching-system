// This file verifies a basic service flow using the test profile.
package com.chesscoach.main;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import com.chesscoach.main.dto.analytics.DashboardAnalyticsResponse;
import com.chesscoach.main.dto.attendance.AttendanceReportResponse;
import com.chesscoach.main.dto.chesscom.ChessComRatingResponse;
import com.chesscoach.main.dto.match.MatchCreateRequest;
import com.chesscoach.main.dto.match.MatchResultRequest;
import com.chesscoach.main.dto.match.MatchSummaryResponse;
import com.chesscoach.main.dto.report.ReportImportResponse;
import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;
import com.chesscoach.main.service.AnalyticsService;
import com.chesscoach.main.service.AttendanceService;
import com.chesscoach.main.service.ChessComService;
import com.chesscoach.main.service.MatchService;
import com.chesscoach.main.service.ReportService;
import com.chesscoach.main.service.TraineeService;

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
                    if (!"norating-user".equals(username)) {
                        response.setRapid(1200);
                    }
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

    @Autowired
    private MatchService matchService;

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private ReportService reportService;

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

    @Test
    void createMatchRejectsMoreThanTwoTrainees() {
        TraineeResponse t1 = createTrainee("Alpha One", "alpha1");
        TraineeResponse t2 = createTrainee("Bravo Two", "bravo2");
        TraineeResponse t3 = createTrainee("Charlie Three", "charlie3");

        MatchCreateRequest request = new MatchCreateRequest();
        request.setFormat("SWISS");
        request.setScheduledDate(LocalDate.now());
        request.setTraineeIds(List.of(t1.getId(), t2.getId(), t3.getId()));

        assertThatThrownBy(() -> matchService.createMatch(request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("only 1 or 2 trainees");
    }

    @Test
    void attendanceReportIncludesIdentityEvenWithoutRecords() {
        TraineeResponse trainee = createTrainee("No Attendance", "noattendance");

        List<AttendanceReportResponse> reports = attendanceService.getAttendanceReport(
            LocalDate.now().minusDays(7),
            LocalDate.now(),
            trainee.getId()
        );

        assertThat(reports).hasSize(1);
        AttendanceReportResponse report = reports.getFirst();
        assertThat(report.getTraineeId()).isEqualTo(trainee.getId());
        assertThat(report.getTraineeName()).isEqualTo(trainee.getName());
        assertThat(report.getTotalSessions()).isEqualTo(0);
    }

    @Test
    void importReportContainsRowLevelErrors() {
        String csv = """
            id,name,age,address,gradeLevel,courseStrand,currentRating
            1,Valid Name,15,Addr,10,STEM,1200
            2,Bad Age,not-a-number,Addr,10,STEM,1300
            """;
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "trainees.csv",
            "text/csv",
            csv.getBytes(StandardCharsets.UTF_8)
        );

        ReportImportResponse response = reportService.importTrainees(file);

        assertThat(response.getTotalRows()).isEqualTo(2);
        assertThat(response.getSuccessRows()).isEqualTo(1);
        assertThat(response.getFailedRows()).isEqualTo(1);
        assertThat(response.getErrors()).isNotNull();
        assertThat(response.getErrors()).isNotEmpty();
        assertThat(response.getErrors().getFirst()).contains("Row");
    }

    @Test
    void createTraineeFallsBackToDefaultRatingWhenChessComModesAreMissing() {
        TraineeResponse created = createTrainee("No Rating User", "norating-user");

        assertThat(created.getCurrentRating()).isEqualTo(1200);
        assertThat(created.getHighestRapidRating()).isEqualTo(1200);
        assertThat(created.getHighestBlitzRating()).isEqualTo(1200);
        assertThat(created.getHighestBulletRating()).isEqualTo(1200);
    }

    @Test
    void listTraineesSupportsRankingOrderAscAndDesc() {
        createTrainee("Rank One", "rank-one");
        createTrainee("Rank Two", "rank-two");
        createTrainee("Rank Three", "rank-three");

        List<TraineeResponse> asc = traineeService.list(null, "asc", 20);
        List<TraineeResponse> desc = traineeService.list(null, "desc", 20);

        assertThat(asc).isNotEmpty();
        assertThat(desc).isNotEmpty();
        assertThat(asc).isSortedAccordingTo(Comparator.comparing(TraineeResponse::getRanking));
        assertThat(desc).isSortedAccordingTo(Comparator.comparing(TraineeResponse::getRanking).reversed());
    }

    @Test
    void recordOfflineResultDoesNotChangeRatings() {
        TraineeResponse white = createTrainee("White Player", "white-player");
        TraineeResponse black = createTrainee("Black Player", "black-player");

        MatchCreateRequest createRequest = new MatchCreateRequest();
        createRequest.setFormat("SWISS");
        createRequest.setScheduledDate(LocalDate.now());
        createRequest.setTraineeIds(List.of(white.getId(), black.getId()));
        MatchSummaryResponse match = matchService.createMatch(createRequest);

        MatchResultRequest resultRequest = new MatchResultRequest();
        resultRequest.setMatchId(match.getMatchId());
        resultRequest.setWhiteScore(1.0);
        resultRequest.setBlackScore(0.0);
        matchService.recordResult(resultRequest);

        TraineeResponse updatedWhite = traineeService.getById(white.getId());
        TraineeResponse updatedBlack = traineeService.getById(black.getId());

        assertThat(updatedWhite.getCurrentRating()).isEqualTo(white.getCurrentRating());
        assertThat(updatedBlack.getCurrentRating()).isEqualTo(black.getCurrentRating());
    }

    private TraineeResponse createTrainee(String name, String username) {
        TraineeRequest request = new TraineeRequest();
        request.setName(name);
        request.setAge(16);
        request.setAddress("Test Address");
        request.setGradeLevel("10");
        request.setCourseStrand("STEM");
        request.setChessUsername(username);
        return traineeService.create(request);
    }
}
