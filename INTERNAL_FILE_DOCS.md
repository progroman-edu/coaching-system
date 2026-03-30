# Internal File Documentation

Short map of each maintained file and its purpose.
Note: `target/` files are build outputs and are intentionally excluded.

## Root Files

- `README.md`: Main project documentation and setup guide.
- `INTERNAL_FILE_DOCS.md`: This internal file map.
- `pom.xml`: Maven build config and dependencies.
- `mvnw`: Maven wrapper launcher for Unix-like shells.
- `mvnw.cmd`: Maven wrapper launcher for Windows.
- `HELP.md`: Spring-generated starter help file.
- `Dockerfile`: Container build definition for the app.
- `compose.yaml`: Local multi-container stack (app + MySQL).
- `.dockerignore`: Excludes files from Docker build context.
- `.gitattributes`: Git attribute rules.
- `.gitignore`: Git ignore rules.

## Main Java Entry

- `src/main/java/com/chesscoach/main/MainApplication.java`: Spring Boot application entry point.

## Config (`config`)

- `src/main/java/com/chesscoach/main/config/ApiPaths.java`: Central API route constants.
- `src/main/java/com/chesscoach/main/config/StaticResourceConfig.java`: Maps `/uploads/**` to filesystem storage.

## Controllers (`controller`)

- `src/main/java/com/chesscoach/main/controller/AnalyticsController.java`: Analytics endpoints.
- `src/main/java/com/chesscoach/main/controller/AttendanceController.java`: Attendance endpoints.
- `src/main/java/com/chesscoach/main/controller/MatchController.java`: Match/pairing/result endpoints.
- `src/main/java/com/chesscoach/main/controller/ReportController.java`: Export/import/download report endpoints.
- `src/main/java/com/chesscoach/main/controller/TraineeController.java`: Trainee CRUD and photo upload endpoints.

## DTOs (`dto`)

### Common
- `src/main/java/com/chesscoach/main/dto/common/ApiError.java`: Standard API error item.
- `src/main/java/com/chesscoach/main/dto/common/ApiResponse.java`: Standard API response envelope.
- `src/main/java/com/chesscoach/main/dto/common/ValidationError.java`: Validation error shape helper.

### Analytics
- `src/main/java/com/chesscoach/main/dto/analytics/DashboardAnalyticsResponse.java`: Dashboard KPI response.
- `src/main/java/com/chesscoach/main/dto/analytics/TraineePerformanceResponse.java`: Per-trainee performance response.
- `src/main/java/com/chesscoach/main/dto/analytics/RatingTrendPointResponse.java`: Rating trend point response.

### Attendance
- `src/main/java/com/chesscoach/main/dto/attendance/AttendanceRecordRequest.java`: Attendance record request payload.
- `src/main/java/com/chesscoach/main/dto/attendance/AttendanceReportResponse.java`: Attendance report row payload.

### Match
- `src/main/java/com/chesscoach/main/dto/match/MatchCreateRequest.java`: Match creation payload.
- `src/main/java/com/chesscoach/main/dto/match/MatchGenerationRequest.java`: Pairing generation payload.
- `src/main/java/com/chesscoach/main/dto/match/MatchResultRequest.java`: Match result submission payload.
- `src/main/java/com/chesscoach/main/dto/match/MatchSummaryResponse.java`: Match summary response payload.

### Report
- `src/main/java/com/chesscoach/main/dto/report/ReportExportResponse.java`: Export metadata response.
- `src/main/java/com/chesscoach/main/dto/report/ReportImportResponse.java`: Import result summary response.

### Trainee
- `src/main/java/com/chesscoach/main/dto/trainee/TraineeRequest.java`: Trainee create/update payload.
- `src/main/java/com/chesscoach/main/dto/trainee/TraineeResponse.java`: Trainee response payload.

## Exceptions (`exception`)

- `src/main/java/com/chesscoach/main/exception/ApiExceptionHandler.java`: Global API exception mapping.
- `src/main/java/com/chesscoach/main/exception/ConflictException.java`: Conflict business exception.
- `src/main/java/com/chesscoach/main/exception/ResourceNotFoundException.java`: Not-found business exception.

## Domain Models (`model`)

- `src/main/java/com/chesscoach/main/model/AuditableEntity.java`: Shared created/updated timestamp fields.
- `src/main/java/com/chesscoach/main/model/Coach.java`: Coach entity.
- `src/main/java/com/chesscoach/main/model/Trainee.java`: Trainee entity.
- `src/main/java/com/chesscoach/main/model/Attendance.java`: Attendance entity.
- `src/main/java/com/chesscoach/main/model/Match.java`: Match entity.
- `src/main/java/com/chesscoach/main/model/MatchParticipant.java`: Match participant entity.
- `src/main/java/com/chesscoach/main/model/MatchResult.java`: Match result entity.
- `src/main/java/com/chesscoach/main/model/RatingsHistory.java`: Rating history entity.
- `src/main/java/com/chesscoach/main/model/Notification.java`: Notification entity.
- `src/main/java/com/chesscoach/main/model/MatchFormat.java`: Match format enum.
- `src/main/java/com/chesscoach/main/model/MatchStatus.java`: Match status enum.
- `src/main/java/com/chesscoach/main/model/MatchResultType.java`: Match result type enum.
- `src/main/java/com/chesscoach/main/model/NotificationType.java`: Notification type enum.
- `src/main/java/com/chesscoach/main/model/PieceColor.java`: Chess color enum for participants.

## Repositories (`repository`)

- `src/main/java/com/chesscoach/main/repository/CoachRepository.java`: Coach persistence access.
- `src/main/java/com/chesscoach/main/repository/TraineeRepository.java`: Trainee persistence and filter queries.
- `src/main/java/com/chesscoach/main/repository/AttendanceRepository.java`: Attendance persistence/report queries.
- `src/main/java/com/chesscoach/main/repository/MatchRepository.java`: Match persistence/scheduling queries.
- `src/main/java/com/chesscoach/main/repository/MatchParticipantRepository.java`: Participant persistence queries.
- `src/main/java/com/chesscoach/main/repository/MatchResultRepository.java`: Match result persistence/history queries.
- `src/main/java/com/chesscoach/main/repository/RatingsHistoryRepository.java`: Rating history queries.
- `src/main/java/com/chesscoach/main/repository/NotificationRepository.java`: Notification queries.

## Services (`service`)

- `src/main/java/com/chesscoach/main/service/AnalyticsService.java`: Analytics business contract.
- `src/main/java/com/chesscoach/main/service/AttendanceService.java`: Attendance business contract.
- `src/main/java/com/chesscoach/main/service/ImageStorageService.java`: Image storage contract.
- `src/main/java/com/chesscoach/main/service/MatchService.java`: Matchmaking/result business contract.
- `src/main/java/com/chesscoach/main/service/RatingService.java`: Rating update business contract.
- `src/main/java/com/chesscoach/main/service/ReportService.java`: Report import/export business contract.
- `src/main/java/com/chesscoach/main/service/TraineeService.java`: Trainee business contract.

## Service Implementations (`service/impl`)

- `src/main/java/com/chesscoach/main/service/impl/AnalyticsServiceImpl.java`: Analytics calculations implementation.
- `src/main/java/com/chesscoach/main/service/impl/AttendanceServiceImpl.java`: Attendance record/report implementation.
- `src/main/java/com/chesscoach/main/service/impl/LocalImageStorageServiceImpl.java`: Local disk image storage implementation.
- `src/main/java/com/chesscoach/main/service/impl/MatchServiceImpl.java`: Match creation, pairing, and result flow implementation.
- `src/main/java/com/chesscoach/main/service/impl/RatingServiceImpl.java`: ELO/ranking update implementation.
- `src/main/java/com/chesscoach/main/service/impl/ReportServiceImpl.java`: CSV export/import implementation.
- `src/main/java/com/chesscoach/main/service/impl/TraineeServiceImpl.java`: Trainee CRUD implementation.

## Utilities (`util`)

- `src/main/java/com/chesscoach/main/util/EloRatingCalculator.java`: Simplified ELO formula helper.
- `src/main/java/com/chesscoach/main/util/RoundRobinGenerator.java`: Round-robin pairing generator helper.
- `src/main/java/com/chesscoach/main/util/SwissPairingGenerator.java`: Swiss-style pairing helper.

## Resources

- `src/main/resources/application.properties`: Runtime app configuration.
- `src/main/resources/db/schema-chess_coach_db.sql`: SQL schema reference script.
- `src/main/resources/db/migrate_rename_course_strand_to_department.sql`: Renames legacy `course_strand` to `department`.

### Static Frontend
- `src/main/resources/static/index.html`: Dashboard page.
- `src/main/resources/static/trainees.html`: Trainee management page.
- `src/main/resources/static/matches.html`: Matches page.
- `src/main/resources/static/attendance.html`: Attendance page.
- `src/main/resources/static/reports.html`: Reports page.
- `src/main/resources/static/css/styles.css`: Shared frontend styles.
- `src/main/resources/static/js/api.js`: Shared API client using fetch.
- `src/main/resources/static/js/ui.js`: Shared UI helper functions.
- `src/main/resources/static/js/analytics.js`: Dashboard page script.
- `src/main/resources/static/js/trainees.js`: Trainee page script.
- `src/main/resources/static/js/matches.js`: Matches page script.
- `src/main/resources/static/js/attendance.js`: Attendance page script.
- `src/main/resources/static/js/reports.js`: Reports page script.

## Tests

- `src/test/resources/application-test.properties`: Test profile configuration.
- `src/test/java/com/chesscoach/main/MainApplicationTests.java`: Context load test.
- `src/test/java/com/chesscoach/main/ServiceSmokeTest.java`: Basic service integration smoke test.
