# Chess Coach Management System (`main`)

This project is a Spring Boot backend for a chess coach management system.
It currently includes project structure, REST API architecture scaffolding, and database schema/entity mappings.

## Run The Project (Step-by-Step)

### Option A: Run Locally (VS Code + MySQL)

1. Create the database in MySQL Workbench:

```sql
CREATE DATABASE chess_coach_db;
```

2. Set environment variables in PowerShell:

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/chess_coach_db"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="pass123"
```

3. Start the app:

```powershell
.\mvnw spring-boot:run
```

4. Open in browser:

- `http://localhost:8080`

### Option B: Run With Docker Compose

1. Build and run containers:

```powershell
docker compose up --build
```

2. Open in browser:

- `http://localhost:8080`

### Verify Build and Tests

```powershell
.\mvnw test
.\mvnw -DskipTests package
```

Jar output:

- `target/main-0.0.1-SNAPSHOT.jar`

## Current Project Identity

- Project name/artifact: `main`
- Base package: `com.chesscoach.main`
- Main class: `MainApplication`
- Database name: `chess_coach_db`
- Java version: `25`

## Tech Stack

- Java + Spring Boot + Maven
- REST API + JSON
- MySQL + Spring Data JPA
- Validation (`jakarta.validation`)
- Optional development helpers already in `pom.xml`:
  - Lombok
  - DevTools

## What Has Been Implemented

### Step 1 (Environment + Initialization) Status

Implemented/configured in project:

- Standard Maven Spring Boot project layout
- Base package renamed to `com.chesscoach.main`
- Static folders prepared:
  - `src/main/resources/static/img`
  - `src/main/resources/static/css`
  - `src/main/resources/static/js`
- `application.properties` configured for:
  - datasource URL/credentials
  - JPA auto-update and SQL logging
  - multipart upload limits (5MB)

File:

- `src/main/resources/application.properties`

### Step 2 (REST API Architecture + JSON Contract)

Implemented:

- API base path constants
- Unified API response envelope
- Unified API error payloads
- Global exception handling
- DTO contracts for all core modules
- Controller endpoint scaffolding under `/api/*`

#### Base Path Constants

- `src/main/java/com/chesscoach/main/config/ApiPaths.java`

Paths:

- `/api/trainees`
- `/api/attendance`
- `/api/matches`
- `/api/analytics`
- `/api/reports`

#### Common Response/Error Models

- `src/main/java/com/chesscoach/main/dto/common/ApiResponse.java`
- `src/main/java/com/chesscoach/main/dto/common/ApiError.java`
- `src/main/java/com/chesscoach/main/dto/common/ValidationError.java`

#### Global Exception Handling

- `src/main/java/com/chesscoach/main/exception/ApiExceptionHandler.java`
- `src/main/java/com/chesscoach/main/exception/ResourceNotFoundException.java`
- `src/main/java/com/chesscoach/main/exception/ConflictException.java`

#### DTOs Added

Trainee:

- `src/main/java/com/chesscoach/main/dto/trainee/TraineeRequest.java`
- `src/main/java/com/chesscoach/main/dto/trainee/TraineeResponse.java`

Attendance:

- `src/main/java/com/chesscoach/main/dto/attendance/AttendanceRecordRequest.java`
- `src/main/java/com/chesscoach/main/dto/attendance/AttendanceReportResponse.java`

Match:

- `src/main/java/com/chesscoach/main/dto/match/MatchCreateRequest.java`
- `src/main/java/com/chesscoach/main/dto/match/MatchGenerationRequest.java`
- `src/main/java/com/chesscoach/main/dto/match/MatchResultRequest.java`
- `src/main/java/com/chesscoach/main/dto/match/MatchSummaryResponse.java`

Analytics:

- `src/main/java/com/chesscoach/main/dto/analytics/DashboardAnalyticsResponse.java`
- `src/main/java/com/chesscoach/main/dto/analytics/TraineePerformanceResponse.java`

Reports:

- `src/main/java/com/chesscoach/main/dto/report/ReportExportResponse.java`
- `src/main/java/com/chesscoach/main/dto/report/ReportImportResponse.java`

#### Controllers Added

- `src/main/java/com/chesscoach/main/controller/TraineeController.java`
- `src/main/java/com/chesscoach/main/controller/AttendanceController.java`
- `src/main/java/com/chesscoach/main/controller/MatchController.java`
- `src/main/java/com/chesscoach/main/controller/AnalyticsController.java`
- `src/main/java/com/chesscoach/main/controller/ReportController.java`

Current endpoint scaffolding:

- `POST /api/trainees`
- `GET /api/trainees`
- `GET /api/trainees/{id}`
- `PUT /api/trainees/{id}`
- `DELETE /api/trainees/{id}`

- `POST /api/attendance`
- `GET /api/attendance/report`

- `POST /api/matches`
- `POST /api/matches/generate/swiss`
- `POST /api/matches/generate/round-robin`
- `POST /api/matches/result`
- `GET /api/matches/history/{traineeId}`

- `GET /api/analytics/dashboard`
- `GET /api/analytics/performance/{traineeId}`

- `GET /api/reports/export`
- `POST /api/reports/import/trainees`

### Step 3 (Database Schema + Entity Relationships)

Implemented:

- Full JPA entities for core domain
- Foreign-key relationships and cardinality mappings
- Table indexes and uniqueness constraints
- Enum types for controlled values
- SQL schema reference script

#### Core Entities Added

- `src/main/java/com/chesscoach/main/model/Coach.java`
- `src/main/java/com/chesscoach/main/model/Trainee.java`
- `src/main/java/com/chesscoach/main/model/Attendance.java`
- `src/main/java/com/chesscoach/main/model/Match.java`
- `src/main/java/com/chesscoach/main/model/MatchParticipant.java`
- `src/main/java/com/chesscoach/main/model/MatchResult.java`
- `src/main/java/com/chesscoach/main/model/RatingsHistory.java`
- `src/main/java/com/chesscoach/main/model/Notification.java`

Shared timestamp fields:

- `src/main/java/com/chesscoach/main/model/AuditableEntity.java`

Enums:

- `src/main/java/com/chesscoach/main/model/MatchFormat.java`
- `src/main/java/com/chesscoach/main/model/MatchStatus.java`
- `src/main/java/com/chesscoach/main/model/PieceColor.java`
- `src/main/java/com/chesscoach/main/model/MatchResultType.java`
- `src/main/java/com/chesscoach/main/model/NotificationType.java`

SQL script:

- `src/main/resources/db/schema-chess_coach_db.sql`

### Step 4 (Repository Layer + Data Access Contracts)

Implemented:

- Full `JpaRepository` interfaces for all core entities
- Query method contracts for filtering, reports, match history, and notifications
- Custom trainee search query for rating/age/course-strand filters with pagination support

#### Repositories Added

- `src/main/java/com/chesscoach/main/repository/CoachRepository.java`
- `src/main/java/com/chesscoach/main/repository/TraineeRepository.java`
- `src/main/java/com/chesscoach/main/repository/AttendanceRepository.java`
- `src/main/java/com/chesscoach/main/repository/MatchRepository.java`
- `src/main/java/com/chesscoach/main/repository/MatchParticipantRepository.java`
- `src/main/java/com/chesscoach/main/repository/MatchResultRepository.java`
- `src/main/java/com/chesscoach/main/repository/RatingsHistoryRepository.java`
- `src/main/java/com/chesscoach/main/repository/NotificationRepository.java`

#### Query Coverage Added

- Trainee search/filter:
  - rating range
  - age range
  - course/strand text match
  - pageable output
- Attendance:
  - per trainee per date
  - trainee date-range reports
  - overall date-range reports
- Match:
  - by format and round
  - upcoming scheduled by status/date
  - participant lookup by match/trainee
  - match result history by match and by trainee
- Ratings history:
  - by trainee ordered by newest
- Notifications:
  - unread by trainee
  - unread by type
- due scheduled notifications (not yet sent)

### Step 5 (Service Layer + Core Business Logic)

Implemented:

- Service interfaces for all active modules
- Service implementations with transactional business logic
- Matchmaking utilities for Swiss and Round Robin pair generation
- Simplified ELO rating update workflow during match result processing
- Controllers now delegate to services instead of returning static placeholders

#### Service Interfaces Added

- `src/main/java/com/chesscoach/main/service/TraineeService.java`
- `src/main/java/com/chesscoach/main/service/AttendanceService.java`
- `src/main/java/com/chesscoach/main/service/MatchService.java`
- `src/main/java/com/chesscoach/main/service/AnalyticsService.java`
- `src/main/java/com/chesscoach/main/service/ReportService.java`

#### Service Implementations Added

- `src/main/java/com/chesscoach/main/service/impl/TraineeServiceImpl.java`
- `src/main/java/com/chesscoach/main/service/impl/AttendanceServiceImpl.java`
- `src/main/java/com/chesscoach/main/service/impl/MatchServiceImpl.java`
- `src/main/java/com/chesscoach/main/service/impl/AnalyticsServiceImpl.java`
- `src/main/java/com/chesscoach/main/service/impl/ReportServiceImpl.java`

#### Utility Logic Added

- `src/main/java/com/chesscoach/main/util/EloRatingCalculator.java`
- `src/main/java/com/chesscoach/main/util/SwissPairingGenerator.java`
- `src/main/java/com/chesscoach/main/util/RoundRobinGenerator.java`

#### Core Behaviors Implemented

- Trainee:
  - create/list/get/update/delete connected to DB
  - filter + pageable list integration with repository search query
- Attendance:
  - upsert behavior per trainee/date
  - report generation by trainee or global date range
- Match:
  - create match records and participants
  - generate Swiss pairings and Round Robin round pairings
  - save match results
  - auto-update ratings using ELO formula
  - create ratings history entries
  - mark matches as completed after result submission
- Analytics:
  - dashboard metrics (total trainees, average rating, attendance %, matches played)
  - trainee performance summary (W/D/L, ratings, attendance)
- Reports:
  - export metadata contract response
  - basic import file row counting for trainee CSV intake flow

#### Controllers Updated to Use Services

- `src/main/java/com/chesscoach/main/controller/TraineeController.java`
- `src/main/java/com/chesscoach/main/controller/AttendanceController.java`
- `src/main/java/com/chesscoach/main/controller/MatchController.java`
- `src/main/java/com/chesscoach/main/controller/AnalyticsController.java`
- `src/main/java/com/chesscoach/main/controller/ReportController.java`

### Step 6 (Simplified ELO Rating System Integration)

Implemented:

- Dedicated rating service to centralize ELO logic
- Configurable ELO K-factor via application properties
- Automatic rating history writes for both players after each recorded match result
- Automatic ranking recomputation after each rating update
- Match result flow refactored to use the new rating service

#### Rating Service Added

- `src/main/java/com/chesscoach/main/service/RatingService.java`
- `src/main/java/com/chesscoach/main/service/impl/RatingServiceImpl.java`

#### Match Flow Integration Updated

- `src/main/java/com/chesscoach/main/service/impl/MatchServiceImpl.java`

The match result workflow now:

1. Saves result (`match_results`).
2. Calls `RatingService` to:
   - compute new ratings (white/black) with ELO formula,
   - update `trainees.current_rating`,
   - update `trainees.highest_rating`,
   - insert `ratings_history` records,
   - recompute `trainees.ranking` by rating leaderboard order.
3. Marks the match as `COMPLETED`.

#### Repository Contract Extended

- `src/main/java/com/chesscoach/main/repository/TraineeRepository.java`

Added:

- `findAllByOrderByCurrentRatingDescIdAsc()` for deterministic ranking recomputation.

#### ELO Configuration Added

- `src/main/resources/application.properties`

Property:

- `app.rating.k-factor=20`

### Step 7 (Trainee Image Upload Integration)

Implemented:

- Dedicated image storage service for trainee photos
- New trainee photo upload endpoint (`multipart/form-data`)
- Local filesystem upload directory configuration
- Static resource mapping to serve uploaded files from `/uploads/**`
- Upload exception handling wired into global API exception handler

#### Service Additions

- `src/main/java/com/chesscoach/main/service/ImageStorageService.java`
- `src/main/java/com/chesscoach/main/service/impl/LocalImageStorageServiceImpl.java`

Behavior:

- Accepts image files only (`jpg`, `jpeg`, `png`, `webp`)
- Validates content type starts with `image/`
- Stores file under local folder:
  - `${app.upload.base-dir}/${app.upload.trainee-photo-subdir}`
- Returns stored web path:
  - `/uploads/trainee-photos/{generated-file}`

#### Trainee Service/Controller Integration

Updated files:

- `src/main/java/com/chesscoach/main/service/TraineeService.java`
- `src/main/java/com/chesscoach/main/service/impl/TraineeServiceImpl.java`
- `src/main/java/com/chesscoach/main/controller/TraineeController.java`

New endpoint:

- `POST /api/trainees/{id}/photo` (multipart field name: `file`)

Result:

- Uploaded file path is persisted into `trainees.photo_path`
- Endpoint returns updated trainee payload in standard API response envelope

#### Static File Serving

- `src/main/java/com/chesscoach/main/config/StaticResourceConfig.java`

Mapping:

- URL: `/uploads/**`
- Source: local filesystem directory from `app.upload.base-dir`

#### Upload Error Handling

Updated:

- `src/main/java/com/chesscoach/main/exception/ApiExceptionHandler.java`

Now handles:

- `MaxUploadSizeExceededException`
- `MultipartException`

#### Upload Config Added

- `src/main/resources/application.properties`

Properties:

- `app.upload.base-dir=uploads`
- `app.upload.trainee-photo-subdir=trainee-photos`

### Step 8 (Frontend Structure + fetch() API Integration)

Implemented:

- Multi-page frontend structure in `src/main/resources/static`
- Shared responsive CSS and layout/navigation
- Modular JavaScript files using `fetch()` for backend API calls
- Forms and tables wired to your current REST endpoints

#### Frontend Pages Added

- `src/main/resources/static/index.html` (Dashboard)
- `src/main/resources/static/trainees.html` (Trainee CRUD + photo upload + filters)
- `src/main/resources/static/matches.html` (Scheduling, pairing generation, results, history)
- `src/main/resources/static/attendance.html` (Attendance record + report)
- `src/main/resources/static/reports.html` (Export/import controls)

#### Frontend Assets Added

- `src/main/resources/static/css/styles.css`
- `src/main/resources/static/js/api.js`
- `src/main/resources/static/js/ui.js`
- `src/main/resources/static/js/analytics.js`
- `src/main/resources/static/js/trainees.js`
- `src/main/resources/static/js/matches.js`
- `src/main/resources/static/js/attendance.js`
- `src/main/resources/static/js/reports.js`

#### API Connectivity Pattern (fetch)

- All browser calls go through `js/api.js` with base path `/api`
- Standard JSON calls and multipart handling are both supported
- Page modules call only these API helper functions, keeping UI logic separated from transport logic

#### Responsive UI Notes

- Sidebar + content two-column layout for desktop
- Auto-collapse to single-column on mobile widths
- Consistent panels/forms/tables and message banner styling across pages

### Step 9 (Reporting + Import/Export + Analytics Enhancements)

Implemented:

- Real CSV report file generation for trainees, attendance, and matches
- CSV trainee import now persists/upserts records in database
- Report download endpoint for generated files
- Rating trend analytics endpoint for chart-ready frontend usage
- Dashboard page updated with rating trend preview loader

#### Reporting Service Upgrade

Updated:

- `src/main/java/com/chesscoach/main/service/impl/ReportServiceImpl.java`

New behavior:

- `GET /api/reports/export?type=...&format=csv`
  - generates a CSV file under local export directory
  - returns metadata including `downloadPath`
- supported export `type` values:
  - `trainees`
  - `attendance`
  - `matches`
- CSV import (`POST /api/reports/import/trainees`) now:
  - parses CSV rows
  - validates required columns
  - upserts trainees by `(name, default coach)`
  - returns total/success/failed row counts

Current import column order:

- `name,age,address,gradeLevel,courseStrand,currentRating,highestRating,ranking,photoPath`

#### Report Download Endpoint

Updated:

- `src/main/java/com/chesscoach/main/controller/ReportController.java`

Added:

- `GET /api/reports/download/{fileName}`

Security handling included:

- blocks path traversal by validating resolved path stays inside report export directory

#### Report Export Config

Updated:

- `src/main/resources/application.properties`

Added:

- `app.report.export-dir=uploads/reports`

#### Analytics Extension (Rating Improvement Trend)

Added:

- `src/main/java/com/chesscoach/main/dto/analytics/RatingTrendPointResponse.java`

Updated:

- `src/main/java/com/chesscoach/main/service/AnalyticsService.java`
- `src/main/java/com/chesscoach/main/service/impl/AnalyticsServiceImpl.java`
- `src/main/java/com/chesscoach/main/controller/AnalyticsController.java`

New endpoint:

- `GET /api/analytics/rating-trend/{traineeId}`

Output:

- ordered trend points (`timestamp`, `rating`, `change`) sourced from `ratings_history`

#### Frontend Step 9 Updates

Updated:

- `src/main/resources/static/js/api.js`
- `src/main/resources/static/js/analytics.js`
- `src/main/resources/static/index.html`
- `src/main/resources/static/js/reports.js`

Added UX:

- dashboard rating trend preview form
- report export output now includes direct download URL

## Relationship Summary

- One `Coach` -> many `Trainee`
- One `Trainee` -> many `Attendance`
- One `Trainee` -> many `RatingsHistory`
- One `Trainee` -> many `Notification`
- One `Match` -> many `MatchParticipant`
- One `Match` -> many `MatchResult`
- `RatingsHistory` optionally references `MatchResult`

## Validation Performed

Compilation check completed successfully:

```powershell
.\mvnw -DskipTests compile
```

Note: there is a Lombok/JDK 25 warning from `sun.misc.Unsafe`, but build succeeds.

## Current Scope

This repository currently contains architecture scaffolding and data model setup.
Core service logic is implemented. Some advanced behaviors (fine-grained matchmaking constraints, robust CSV parsing, notifications delivery workflows, and full frontend integration) are pending.

## Next Logical Steps

1. Add stronger validation and domain rules for pairing edge cases and import schema mismatches.
2. Add endpoint-level authorization strategy if multi-user support is introduced later.
3. Add database migration strategy (optional, e.g., Flyway) if you want versioned schema changes.
4. Add automated tests for service and controller behavior (especially CSV import/export paths).
5. Add charting/visual analytics rendering for trend data.
6. Add optional AI and chess-engine integrations in later planned steps.
7. Improve UI/UX

## Chess.com PubAPI Integration (Ready)

### What Was Added

- Backend integration service for Chess.com public endpoints.
- Controller endpoints under `/api/chesscom`.
- Trainee support for `chessUsername`.
- Rating sync endpoint to update trainee rating from Chess.com.

### Config

Set in `src/main/resources/application.properties`:

- `app.chesscom.base-url` (default: `https://api.chess.com/pub`)
- `app.chesscom.user-agent`
- `app.chesscom.timeout-seconds`

### Endpoints

- `GET /api/chesscom/{username}/rating`
- `GET /api/chesscom/{username}/match-history/archives`
- `GET /api/chesscom/{username}/match-history/{year}/{month}`
- `GET /api/chesscom/{username}/match-history/all-modes?limitArchives=24`
- `POST /api/chesscom/trainees/{traineeId}/sync-rating?mode=rapid|blitz|bullet`

### Notes

- PubAPI is read-only.
- The sync endpoint stores a `ratings_history` entry and updates trainee current/highest rating.
- Supported sync modes: `rapid`, `blitz`, `bullet`, `puzzles`, `puzzle_rush`.
