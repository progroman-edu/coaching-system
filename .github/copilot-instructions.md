# Coaching System Project Guidelines

## Overview

The coaching-system is a Spring Boot application designed to manage chess coaching workflows. See `STRATEGIC_ROADMAP.md` for high-level system design and planned features.

## Build and Test

**Install dependencies and build:**
```bash
mvn clean install
```

**Run tests:**
```bash
mvn test
```

**Run the application:**
```bash
./mvnw spring-boot:run
# or on Windows:
mvnw.cmd spring-boot:run
```

**Build Docker image:**
```bash
docker build -t coaching-system .
docker-compose up  # uses compose.yaml
```

## Code Style and Conventions

- **Language**: Java 25
- **Framework**: Spring Boot 4.0.3
- **Build Tool**: Maven (pom.xml)
- **Package structure**: `com.chesscoach.*`
- Follow Spring Boot best practices: dependency injection, annotations, layered architecture
- Use meaningful variable names and minimal comments (only when logic needs clarification)

## Database

- Database migrations stored in `src/main/resources/db/migration/` (Flyway convention)
- Always test migrations against actual schema before committing
- Document breaking changes in the migration file header

## Architecture

Key components:
- See `entity and relationship.md` for data model
- See `INTERNAL_FILE_DOCS.md` for detailed component documentation
- See `STRATEGIC_ROADMAP.md` for planned features and technical decisions

## Before Committing

1. Run `mvn clean install` to ensure build succeeds
2. Run `mvn test` to verify all tests pass
3. If database changes were made, test the migration script
4. Include meaningful commit messages with the format: `[Category] Brief description`
   - Example: `[Feature] Add user authentication module`
   - Example: `[Fix] Resolve NullPointerException in UserService`
   - Example: `[Database] Add sessions table migration`

## Useful Files

- `README.md` — Project overview and setup
- `pom.xml` — Maven dependencies and build configuration
- `Dockerfile` — Container build configuration
- `compose.yaml` — Docker Compose for local development
- `src/` — Main source code
- `tools/` — Utility scripts and tools
