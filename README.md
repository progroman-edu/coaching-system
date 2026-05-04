# Chess Coach Management System (`main`)

Spring Boot application for managing chess trainees, attendance, matches, analytics, reports, and Chess.com rating sync.

## Tech Stack

- Java 25
- Spring Boot 4.0.3
- Spring Web MVC + Validation + Data JPA
- MySQL (runtime)
- H2 (tests)
- Maven
- Static frontend (HTML/CSS/JS under `src/main/resources/static`)

## What Is Implemented

- Trainee management: CRUD, filtering, image upload
- Attendance: record and date-range reports
- Matches: create, Swiss pairing, Round Robin pairing, result recording
- Rating system: ELO updates + rating history + ranking recomputation
- Analytics: dashboard, trainee performance, rating trend
- Reports: CSV export, trainee CSV import, file download
- Chess.com: ratings lookup, archives/history retrieval, trainee rating sync

## API Overview

Base path: `/api/v1`

### Trainees

- `POST /api/v1/trainees`
- `GET /api/v1/trainees`
- `GET /api/v1/trainees/{id}`
- `PUT /api/v1/trainees/{id}`
- `POST /api/v1/trainees/{id}/photo` (multipart field: `file`)
- `DELETE /api/v1/trainees/{id}`

### Attendance

- `POST /api/v1/attendance`
- `GET /api/v1/attendance/report?startDate=YYYY-MM-DD&endDate=YYYY-MM-DD&traineeId={optional}`

### Matches

- `POST /api/v1/matches`
- `POST /api/v1/matches/generate/swiss`
- `POST /api/v1/matches/generate/round-robin`
- `POST /api/v1/matches/result`
- `GET /api/v1/matches/history/{traineeId}`

### Analytics

- `GET /api/v1/analytics/dashboard`
- `GET /api/v1/analytics/performance/{traineeId}`
- `GET /api/v1/analytics/rating-trend/{traineeId}`

### Reports

- `GET /api/v1/reports/export?type=trainees|attendance|matches&format=csv`
- `POST /api/v1/reports/import/trainees` (multipart field: `file`)
- `GET /api/v1/reports/download/{fileName}`

### Chess.com

- `GET /api/v1/chesscom/{username}/rating`
- `GET /api/v1/chesscom/{username}/match-history/archives`
- `GET /api/v1/chesscom/{username}/match-history/{year}/{month}`
- `GET /api/v1/chesscom/{username}/match-history/all-modes?limitArchives={optional}`
- `POST /api/v1/chesscom/trainees/{traineeId}/sync-rating?mode=rapid|blitz|bullet`

Note: Chess.com sync currently supports only `rapid`, `blitz`, and `bullet`.

## Run Locally

1. Create database:

```sql
CREATE DATABASE chess_coach_db;
```

2. Set up environment variables:

**Option A: Using `.env` file (recommended)**
```bash
# Copy template
cp .env.example .env

# Edit .env with your database credentials and personal info:
# APP_DB_PASSWORD=your_password
# APP_COACH_DEFAULT_EMAIL=your-email@example.com
# APP_COACH_DEFAULT_NAME=Your Name
# etc.
```

**Option B: Set manually via PowerShell**
```powershell
$env:APP_DB_URL="jdbc:mysql://localhost:3306/chess_coach_db?serverTimezone=UTC"
$env:APP_DB_USERNAME="root"
$env:APP_DB_PASSWORD="your_password"
$env:APP_COACH_DEFAULT_EMAIL="your-email@example.com"
$env:APP_COACH_DEFAULT_NAME="Your Name"
$env:APP_COACH_DEFAULT_PHONE="+1-XXX-XXX-XXXX"
$env:APP_CHESSCOM_USER_AGENT="ChessCoachMain/1.0 (contact: your-email@example.com)"
```

3. Start app:

```powershell
.\mvnw spring-boot:run
```

### If startup fails with missing `highest_*_rating` columns

If you are running against an older `chess_coach_db` schema and see Hibernate validation errors like:
`missing column [highest_blitz_rating] in table [trainees]`,
run:

```sql
SOURCE src/main/resources/db/migrate_add_trainee_mode_rating_columns.sql;
```

Then restart the app.

### If startup fails with missing `department` column

If you are running against an older `chess_coach_db` schema that still uses `course_strand`,
run:

```sql
SOURCE src/main/resources/db/migrate_rename_course_strand_to_department.sql;
```

Then restart the app.

### If existing trainee usernames have uppercase letters

Run:

```sql
SOURCE src/main/resources/db/migrate_lowercase_trainee_chess_usernames.sql;
```

4. Open:

- `http://localhost:8080`

## Run With Docker Compose

```powershell
docker compose up --build
```

Services:
- App: `http://localhost:8080`
- MySQL: `localhost:3306`

Compose defaults:
- DB name: `chess_coach_db`
- DB user: from `MYSQL_USER` (default `chesscoach`)
- DB password: required `MYSQL_PASSWORD`
- Root password: required `MYSQL_ROOT_PASSWORD`

## Build and Test

```powershell
.\mvnw test
.\mvnw -DskipTests package
```

Jar output:
- `target/main-0.0.1-SNAPSHOT.jar`

## Key Configuration

From `src/main/resources/application.properties`:

- `spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`
- `spring.jpa.hibernate.ddl-auto` (defaults to `validate`, override via `SPRING_JPA_HIBERNATE_DDL_AUTO`)
- `spring.servlet.multipart.max-file-size=5MB`
- `app.rating.k-factor=20`
- `app.upload.base-dir` (default `uploads`)
- `app.upload.trainee-photo-subdir` (default `trainee-photos`)
- `app.report.export-dir` (default `uploads/reports`)
- `app.chesscom.base-url` (default `https://api.chess.com/pub`)
- `app.chesscom.user-agent`
- `app.chesscom.timeout-seconds`
- `app.chesscom.max-archives`

## Test Profile

`src/test/resources/application-test.properties` uses:

- In-memory H2 database (`MODE=MySQL`)
- `ddl-auto=create-drop`
- Local test upload directories under `target/test-uploads`

## Project Layout

- `src/main/java/com/chesscoach/main/controller`: REST controllers
- `src/main/java/com/chesscoach/main/service`: service contracts
- `src/main/java/com/chesscoach/main/service/impl`: business logic
- `src/main/java/com/chesscoach/main/repository`: JPA repositories
- `src/main/java/com/chesscoach/main/model`: JPA entities/enums
- `src/main/resources/static`: frontend pages/assets
- `src/main/resources/db/schema-chess_coach_db.sql`: SQL schema reference
- `src/main/resources/db/migrate_rename_course_strand_to_department.sql`: Migration for older schemas that still use `course_strand`
