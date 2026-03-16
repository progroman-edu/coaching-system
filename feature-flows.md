# Coaching System Feature Flows (Plain-Text Sequence Steps)

This document summarizes request/response flows and backend/frontend sequences for all major features.
It is based on the current code under `src/main`.

---

## 1) Trainees (CRUD, Filter, Photos, Ratings Sync)

### 1.1 Create Trainee
Sequence:
1. User fills the Create Trainee form on `trainees.html`.
2. Frontend (`trainees.js`) submits payload to `POST /api/trainees` via `api.createTrainee`.
3. `TraineeController.create` validates and forwards to `TraineeService.create`.
4. `TraineeServiceImpl.create`:
   - Creates `Trainee` entity.
   - Assigns default coach (`coach@local`) via `CoachRepository`.
   - Applies request fields.
   - Fetches Chess.com ratings if `chessUsername` is set (`ChessComService.getRatings`).
   - If no rating returned, applies `defaultRating` from `application.properties`.
   - Saves trainee and recomputes rankings.
5. Response: `ApiResponse<TraineeResponse>` with created trainee data.
6. Frontend reloads list and resets form.

Request/Response:
- Request: `POST /api/trainees`
  - Body: `{ name, age, address, gradeLevel, courseStrand, chessUsername, photoPath? }`
- Response: `200 OK`
  - Body: `{ success: true, data: TraineeResponse, ... }`

### 1.2 List/Filter Trainees
Sequence:
1. User submits filter form on `trainees.html`.
2. Frontend (`trainees.js`) calls `GET /api/trainees` with query params.
3. `TraineeController.list` forwards to `TraineeService.list`.
4. `TraineeServiceImpl.list` uses `TraineeRepository.search` with paging/sort.
5. Response: list of trainees.
6. Frontend renders rows.

Request/Response:
- Request: `GET /api/trainees?ratingMin=&ratingMax=&ageMin=&ageMax=&courseStrand=&rankingOrder=&page=&size=`
- Response: `200 OK` with `List<TraineeResponse>`.

### 1.3 Update Trainee
Sequence:
1. User clicks Edit, form populated.
2. User submits updated data.
3. Frontend (`trainees.js`) calls `PUT /api/trainees/{id}`.
4. `TraineeController.update` -> `TraineeService.update`.
5. `TraineeServiceImpl.update` applies new values and saves.
6. Response with updated trainee.

Request/Response:
- Request: `PUT /api/trainees/{id}`
- Response: `200 OK` with updated `TraineeResponse`.

### 1.4 Delete Trainee
Sequence:
1. User clicks Delete.
2. Frontend calls `DELETE /api/trainees/{id}`.
3. `TraineeController.delete` -> `TraineeService.delete`.
4. `TraineeServiceImpl.delete` deletes entity, recomputes rankings, may reset auto-increment.
5. Response: success message.

Request/Response:
- Request: `DELETE /api/trainees/{id}`
- Response: `200 OK` with `ApiResponse<Void>`.

### 1.5 Upload Trainee Photo
Sequence:
1. User selects image file, submits form.
2. Frontend calls `POST /api/trainees/{id}/photo` with multipart form data.
3. `TraineeController.uploadPhoto` -> `TraineeService.updatePhoto`.
4. `LocalImageStorageServiceImpl.saveTraineePhoto` validates extension + signature and writes file to `uploads/trainee-photos`.
5. Photo path is stored on trainee.
6. Response with updated trainee.

Request/Response:
- Request: `POST /api/trainees/{id}/photo` (multipart/form-data)
- Response: `200 OK` with `TraineeResponse`.

### 1.6 Sync Ratings (Chess.com)
Sequence:
1. User clicks “Sync Ratings” in `trainees.html`.
2. Frontend loops through loaded trainees and calls `POST /api/chesscom/trainees/{id}/sync-rating?mode=...`.
3. `ChessComController.syncRating` -> `ChessComService.syncTraineeRating`.
4. `ChessComServiceImpl.syncTraineeRating`:
   - Loads trainee, fetches Chess.com ratings.
   - Resolves selected mode with fallbacks (rapid/blitz/bullet).
   - Updates trainee current/highest rating and mode.
   - Inserts `RatingsHistory` record.
   - Recomputes rankings.
5. Response: `ChessComSyncRatingResponse`.

Request/Response:
- Request: `POST /api/chesscom/trainees/{id}/sync-rating?mode=rapid|blitz|bullet`
- Response: `200 OK` with `ChessComSyncRatingResponse`.

---

## 2) Attendance

### 2.1 Record Attendance
Sequence:
1. User submits attendance form.
2. Frontend calls `POST /api/attendance` with JSON payload.
3. `AttendanceController.recordAttendance` -> `AttendanceService.recordAttendance`.
4. `AttendanceServiceImpl.recordAttendance`:
   - Finds trainee.
   - Upserts attendance record for the date.
   - Saves and returns input.
5. Response: `AttendanceRecordRequest` wrapped in `ApiResponse`.

Request/Response:
- Request: `POST /api/attendance`
  - Body: `{ traineeId, attendanceDate, present, remarks? }`
- Response: `201 Created` with same fields.

### 2.2 Attendance Report
Sequence:
1. User submits report form.
2. Frontend calls `GET /api/attendance/report?startDate=...&endDate=...&traineeId?=...`.
3. `AttendanceController.getAttendanceReport` -> `AttendanceService.getAttendanceReport`.
4. Service aggregates attendance by trainee or a specific trainee.
5. Response: list of `AttendanceReportResponse`.

Request/Response:
- Request: `GET /api/attendance/report?...`
- Response: `200 OK` with list of report rows.

---

## 3) Matches & Ratings

### 3.1 Create Match (Manual Scheduling)
Sequence:
1. User selects participants and date on `matches.html`.
2. Frontend calls `POST /api/matches`.
3. `MatchController.createMatch` -> `MatchService.createMatch`.
4. `MatchServiceImpl.createMatch`:
   - Validates trainee IDs (1–2).
   - Creates `Match` with `SCHEDULED` status.
   - Creates `MatchParticipant` records with colors and board numbers.
5. Response: `MatchSummaryResponse`.

Request/Response:
- Request: `POST /api/matches`
  - Body: `{ scheduledDate, traineeIds, format }`
- Response: `201 Created` with match summary.

### 3.2 Generate Swiss Pairings
Sequence:
1. User selects trainees and round.
2. Frontend calls `POST /api/matches/generate/swiss`.
3. Service sorts trainees by rating and uses `SwissPairingGenerator`.
4. Creates matches + participants for each pairing.
5. Response: list of `MatchSummaryResponse`.

Request/Response:
- Request: `POST /api/matches/generate/swiss`
- Response: `200 OK` with generated matches.

### 3.3 Generate Round Robin Pairings
Sequence:
1. User selects trainees and round.
2. Frontend calls `POST /api/matches/generate/round-robin`.
3. Service uses `RoundRobinGenerator` for the round.
4. Creates matches + participants.
5. Response: list of `MatchSummaryResponse`.

Request/Response:
- Request: `POST /api/matches/generate/round-robin`
- Response: `200 OK` with generated matches.

### 3.4 Record Match Result
Sequence:
1. User selects a match and winner (white/black/draw).
2. Frontend maps to scores and calls `POST /api/matches/result`.
3. `MatchController.recordResult` -> `MatchService.recordResult`.
4. `MatchServiceImpl.recordResult`:
   - Validates no prior result.
   - Validates scores (1-0, 0-1, 0.5-0.5).
   - Loads participants.
   - Writes `MatchResult`.
   - Calls `RatingService.applyMatchResultRatingUpdate`.
   - Marks match `COMPLETED`.
5. Response: `MatchResultRequest`.

Request/Response:
- Request: `POST /api/matches/result`
- Response: `200 OK`.

### 3.5 Match History by Trainee (Offline)
Sequence:
1. User selects trainee profile in history form.
2. Frontend calls `GET /api/matches/history/{traineeId}`.
3. `MatchService.getHistoryByTrainee` returns match results.
4. Frontend renders rows.

Request/Response:
- Request: `GET /api/matches/history/{traineeId}`
- Response: `200 OK` with match summaries.

---

## 4) Analytics

### 4.1 Dashboard KPIs
Sequence:
1. Dashboard page loads.
2. Frontend calls `GET /api/analytics/dashboard`.
3. `AnalyticsServiceImpl.getDashboard` calculates totals, averages, and percentages.
4. Response: `DashboardAnalyticsResponse`.

Request/Response:
- Request: `GET /api/analytics/dashboard`
- Response: `200 OK` with KPIs.

### 4.2 Trainee Performance
Sequence:
1. UI requests performance for a trainee.
2. Frontend calls `GET /api/analytics/performance/{traineeId}`.
3. Service aggregates wins/draws/losses, attendance %, rating info.
4. Response: `TraineePerformanceResponse`.

Request/Response:
- Request: `GET /api/analytics/performance/{traineeId}`
- Response: `200 OK` with performance summary.

### 4.3 Rating Trend
Sequence:
1. UI requests rating trend for a trainee.
2. Frontend calls `GET /api/analytics/rating-trend/{traineeId}`.
3. Service returns chronological `RatingsHistory` points.
4. Response: `List<RatingTrendPointResponse>`.

Request/Response:
- Request: `GET /api/analytics/rating-trend/{traineeId}`
- Response: `200 OK` with time series points.

---

## 5) Reports (Import/Export)

### 5.1 Export CSV
Sequence:
1. User selects report type and format on `reports.html`.
2. Frontend calls `GET /api/reports/export?type=...&format=...`.
3. `ReportServiceImpl.export` creates CSV file in `uploads/reports`.
4. Response includes file name and download path.
5. Frontend renders download link.

Request/Response:
- Request: `GET /api/reports/export?type=trainees|attendance|matches&format=csv`
- Response: `200 OK` with `ReportExportResponse`.

### 5.2 Import Trainees CSV
Sequence:
1. User uploads a CSV file.
2. Frontend calls `POST /api/reports/import/trainees` with multipart file.
3. `ReportServiceImpl.importTrainees` parses file and upserts trainees by name/coach.
4. Response includes counts and error list.

Request/Response:
- Request: `POST /api/reports/import/trainees` (multipart/form-data)
- Response: `200 OK` with `ReportImportResponse`.

### 5.3 Download Exported File
Sequence:
1. User clicks download link.
2. Browser requests `GET /api/reports/download/{fileName}`.
3. `ReportController.download` validates path and serves file.

Request/Response:
- Request: `GET /api/reports/download/{fileName}`
- Response: `200 OK` with file stream.

---

## 6) Chess.com Integration (Online Match History)

### 6.1 Get Ratings
Sequence:
1. Frontend requests ratings for a username.
2. `ChessComServiceImpl.getRatings` calls Chess.com PubAPI `/player/{username}/stats`.
3. Response normalized to `ChessComRatingResponse`.

Request/Response:
- Request: `GET /api/chesscom/{username}/rating`
- Response: `200 OK` with ratings.

### 6.2 Get Archives / Monthly Games
Sequence:
1. Frontend requests archives or a specific month.
2. Service calls Chess.com PubAPI endpoints.
3. Returns raw JSON mapped to `Object`.

Request/Response:
- Request: `GET /api/chesscom/{username}/match-history/archives`
- Request: `GET /api/chesscom/{username}/match-history/{year}/{month}`
- Response: `200 OK` with JSON.

### 6.3 Get All-Modes History (UI Online Mode)
Sequence:
1. User selects “Online” mode in match history.
2. Frontend calls `GET /api/chesscom/{username}/match-history/all-modes?limitArchives=...`.
3. Service fetches archives, then downloads recent months, groups games by time class.
4. Response: grouped JSON for UI rendering.

Request/Response:
- Request: `GET /api/chesscom/{username}/match-history/all-modes?limitArchives=N`
- Response: `200 OK` with grouped data.

---

## Shared API Behavior & Error Handling

- `ApiExceptionHandler` converts thrown exceptions to `ApiResponse.fail(...)` with HTTP status codes.
- Validation errors produce `400 Bad Request` and `VALIDATION_ERROR` entries.
- Resource not found produces `404 Not Found` and `NOT_FOUND`.
- Conflicts produce `409 Conflict` and `CONFLICT`.
- Generic errors produce `500 Internal Server Error` with `INTERNAL_ERROR`.

---

## Frontend Shared Utilities

- `api.js`: central HTTP wrapper, throws on non-OK or `success=false`.
- `ui.js`: message display, HTML escaping, and table rendering helpers.
- `sidebar.js`: toggles sidebar for all pages.

