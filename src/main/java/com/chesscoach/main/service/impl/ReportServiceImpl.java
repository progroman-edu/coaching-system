// This service implementation contains business logic for Report operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.report.ReportExportResponse;
import com.chesscoach.main.dto.report.ReportImportResponse;
import com.chesscoach.main.model.Attendance;
import com.chesscoach.main.model.BlitzRating;
import com.chesscoach.main.model.BulletRating;
import com.chesscoach.main.model.Coach;
import com.chesscoach.main.model.MatchResult;
import com.chesscoach.main.model.RapidRating;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.AttendanceRepository;
import com.chesscoach.main.repository.CoachRepository;
import com.chesscoach.main.repository.MatchResultRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReportServiceImpl implements ReportService {

    private static final String DEFAULT_COACH_EMAIL = "coach@local";
    private static final int MAX_IMPORT_ERRORS = 100;

    private final TraineeRepository traineeRepository;
    private final AttendanceRepository attendanceRepository;
    private final MatchResultRepository matchResultRepository;
    private final CoachRepository coachRepository;

    @Value("${app.report.export-dir:uploads/reports}")
    private String exportDir;

    public ReportServiceImpl(
        TraineeRepository traineeRepository,
        AttendanceRepository attendanceRepository,
        MatchResultRepository matchResultRepository,
        CoachRepository coachRepository
    ) {
        this.traineeRepository = traineeRepository;
        this.attendanceRepository = attendanceRepository;
        this.matchResultRepository = matchResultRepository;
        this.coachRepository = coachRepository;
    }

    @Override
    public ReportExportResponse export(String type, String format) {
        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        String normalizedFormat = format.trim().toLowerCase(Locale.ROOT);
        if (!"csv".equals(normalizedFormat)) {
            throw new IllegalArgumentException("Only CSV export is currently supported");
        }

        String fileName = normalizedType + "-report-" + OffsetDateTime.now().toEpochSecond() + ".csv";
        Path exportPath = Paths.get(exportDir).toAbsolutePath().normalize().resolve(fileName);
        writeCsvExport(normalizedType, exportPath);

        ReportExportResponse response = new ReportExportResponse();
        response.setType(normalizedType);
        response.setFormat(normalizedFormat);
        response.setFileName(fileName);
        response.setDownloadPath("/api/reports/download/" + fileName);
        return response;
    }

    @Override
    public ReportImportResponse importTrainees(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is required");
        }

        int totalRows = 0;
        int successRows = 0;
        int failedRows = 0;
        List<String> errors = new ArrayList<>();

        Coach coach = getOrCreateDefaultCoach();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String header = reader.readLine();
            if (header == null || header.isBlank()) {
                throw new IllegalArgumentException("CSV header row is required");
            }
            CsvColumns columns = CsvColumns.fromHeader(header);

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                totalRows++;
                try {
                    upsertTraineeFromCsv(line, coach, columns);
                    successRows++;
                } catch (Exception ex) {
                    failedRows++;
                    if (errors.size() < MAX_IMPORT_ERRORS) {
                        errors.add("Row " + totalRows + ": " + ex.getMessage());
                    }
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read import file: " + ex.getMessage());
        }

        ReportImportResponse response = new ReportImportResponse();
        response.setFileName(file.getOriginalFilename());
        response.setTotalRows(totalRows);
        response.setSuccessRows(successRows);
        response.setFailedRows(failedRows);
        response.setErrors(errors);
        return response;
    }

    private void writeCsvExport(String type, Path targetPath) {
        try {
            Files.createDirectories(targetPath.getParent());
            List<String> lines = switch (type) {
                case "trainees" -> exportTraineesCsv();
                case "attendance" -> exportAttendanceCsv();
                case "matches" -> exportMatchesCsv();
                default -> throw new IllegalArgumentException("Unsupported report type: " + type);
            };
            Files.write(targetPath, lines, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to write export file: " + ex.getMessage(), ex);
        }
    }

    private List<String> exportTraineesCsv() {
        List<String> lines = new ArrayList<>();
        lines.add("id,name,age,address,gradeLevel,department,rapidCurrentRating,rapidHighestRating,blitzCurrentRating,blitzHighestRating,bulletCurrentRating,bulletHighestRating,ranking,photoPath,chessUsername");
        for (Trainee t : traineeRepository.findAll()) {
            RapidRating rapid = t.getRapidRating();
            BlitzRating blitz = t.getBlitzRating();
            BulletRating bullet = t.getBulletRating();
            lines.add(String.join(",",
                safeCsv(t.getId()),
                safeCsv(t.getName()),
                safeCsv(t.getAge()),
                safeCsv(t.getAddress()),
                safeCsv(t.getGradeLevel()),
                safeCsv(t.getDepartment()),
                safeCsv(rapid != null ? rapid.getCurrentRating() : null),
                safeCsv(rapid != null ? rapid.getHighestRating() : null),
                safeCsv(blitz != null ? blitz.getCurrentRating() : null),
                safeCsv(blitz != null ? blitz.getHighestRating() : null),
                safeCsv(bullet != null ? bullet.getCurrentRating() : null),
                safeCsv(bullet != null ? bullet.getHighestRating() : null),
                safeCsv(t.getRanking()),
                safeCsv(t.getPhotoPath()),
                safeCsv(t.getChessUsername())
            ));
        }
        return lines;
    }

    private List<String> exportAttendanceCsv() {
        List<String> lines = new ArrayList<>();
        lines.add("id,traineeId,traineeName,attendanceDate,present,remarks");
        for (Attendance a : attendanceRepository.findAll()) {
            lines.add(String.join(",",
                safeCsv(a.getId()),
                safeCsv(a.getTrainee() != null ? a.getTrainee().getId() : null),
                safeCsv(a.getTrainee() != null ? a.getTrainee().getName() : null),
                safeCsv(a.getAttendanceDate()),
                safeCsv(a.getPresent()),
                safeCsv(a.getRemarks())
            ));
        }
        return lines;
    }

    private List<String> exportMatchesCsv() {
        List<String> lines = new ArrayList<>();
        lines.add("id,matchId,scheduledDate,format,whiteTraineeId,whiteName,blackTraineeId,blackName,whiteScore,blackScore,resultType,playedAt");
        for (MatchResult r : matchResultRepository.findAll()) {
            lines.add(String.join(",",
                safeCsv(r.getId()),
                safeCsv(r.getMatch() != null ? r.getMatch().getId() : null),
                safeCsv(r.getMatch() != null ? r.getMatch().getScheduledDate() : null),
                safeCsv(r.getMatch() != null && r.getMatch().getFormat() != null ? r.getMatch().getFormat().name() : null),
                safeCsv(r.getWhiteTrainee() != null ? r.getWhiteTrainee().getId() : null),
                safeCsv(r.getWhiteTrainee() != null ? r.getWhiteTrainee().getName() : null),
                safeCsv(r.getBlackTrainee() != null ? r.getBlackTrainee().getId() : null),
                safeCsv(r.getBlackTrainee() != null ? r.getBlackTrainee().getName() : null),
                safeCsv(r.getWhiteScore()),
                safeCsv(r.getBlackScore()),
                safeCsv(r.getResultType() != null ? r.getResultType().name() : null),
                safeCsv(r.getPlayedAt())
            ));
        }
        return lines;
    }

    private void upsertTraineeFromCsv(String line, Coach coach, CsvColumns columns) {
        String[] raw = splitCsv(line);
        if (raw.length == 0) {
            throw new IllegalArgumentException("Invalid CSV row");
        }

        String name = clean(getCell(raw, columns, "name", 0));
        Integer age = parseInt(getCell(raw, columns, "age", 1), true);
        String address = clean(getCell(raw, columns, "address", 2));
        String gradeLevel = clean(getCell(raw, columns, "gradeLevel", 3));
        String department = clean(getCell(raw, columns, "department", 4));
        Integer rapidCurrentRating = parseInt(getCell(raw, columns, "rapidCurrentRating", 5), false);
        Integer rapidHighestRating = parseInt(getCell(raw, columns, "rapidHighestRating", 6), false);
        Integer blitzCurrentRating = parseInt(getCell(raw, columns, "blitzCurrentRating", 7), false);
        Integer blitzHighestRating = parseInt(getCell(raw, columns, "blitzHighestRating", 8), false);
        Integer bulletCurrentRating = parseInt(getCell(raw, columns, "bulletCurrentRating", 9), false);
        Integer bulletHighestRating = parseInt(getCell(raw, columns, "bulletHighestRating", 10), false);
        Integer ranking = parseInt(getCell(raw, columns, "ranking", 11), false);
        String photoPath = clean(getCell(raw, columns, "photoPath", 12));
        String chessUsername = clean(getCell(raw, columns, "chessUsername", 13));

        if (name.isBlank() || address.isBlank() || gradeLevel.isBlank() || department.isBlank()) {
            throw new IllegalArgumentException("Missing required trainee values");
        }

        Optional<Trainee> existing = traineeRepository.findByNameIgnoreCaseAndCoachId(name, coach.getId());
        Trainee trainee = existing.orElseGet(Trainee::new);
        trainee.setCoach(coach);
        trainee.setName(name);
        trainee.setAge(age);
        trainee.setAddress(address);
        trainee.setGradeLevel(gradeLevel);
        trainee.setDepartment(department);
        trainee.setRanking(ranking);
        trainee.setPhotoPath(photoPath.isBlank() ? null : photoPath);
        trainee.setChessUsername(chessUsername.isBlank() ? null : chessUsername);
        
        // Set up rating entities
        RapidRating rapid = trainee.getRapidRating();
        if (rapid == null) {
            rapid = new RapidRating();
            rapid.setTrainee(trainee);
            trainee.setRapidRating(rapid);
        }
        rapid.setCurrentRating(rapidCurrentRating != null ? rapidCurrentRating : 1200);
        rapid.setHighestRating(rapidHighestRating != null ? rapidHighestRating : rapid.getCurrentRating());
        
        BlitzRating blitz = trainee.getBlitzRating();
        if (blitz == null) {
            blitz = new BlitzRating();
            blitz.setTrainee(trainee);
            trainee.setBlitzRating(blitz);
        }
        blitz.setCurrentRating(blitzCurrentRating != null ? blitzCurrentRating : 1200);
        blitz.setHighestRating(blitzHighestRating != null ? blitzHighestRating : blitz.getCurrentRating());
        
        BulletRating bullet = trainee.getBulletRating();
        if (bullet == null) {
            bullet = new BulletRating();
            bullet.setTrainee(trainee);
            trainee.setBulletRating(bullet);
        }
        bullet.setCurrentRating(bulletCurrentRating != null ? bulletCurrentRating : 1200);
        bullet.setHighestRating(bulletHighestRating != null ? bulletHighestRating : bullet.getCurrentRating());
        
        traineeRepository.save(trainee);
    }

    private static String getCell(String[] raw, CsvColumns columns, String header, int fallbackWithoutId) {
        Integer fromHeader = columns.indexByHeader.get(header.toLowerCase(Locale.ROOT));
        int index = fromHeader != null ? fromHeader : fallbackWithoutId + columns.leadingOffset;
        if (index < 0 || index >= raw.length) {
            return "";
        }
        return raw[index];
    }

    private Coach getOrCreateDefaultCoach() {
        return coachRepository.findByEmail(DEFAULT_COACH_EMAIL)
            .orElseGet(() -> {
                Coach coach = new Coach();
                coach.setFullName("Default Coach");
                coach.setEmail(DEFAULT_COACH_EMAIL);
                coach.setPhone("N/A");
                return coachRepository.save(coach);
            });
    }

    private static String[] splitCsv(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells.toArray(String[]::new);
    }

    private static Integer parseInt(String value, boolean required) {
        String clean = clean(value);
        if (clean.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("Missing required numeric value");
            }
            return null;
        }
        return Integer.parseInt(clean);
    }

    private static String clean(Object value) {
        return Objects.toString(value, "").trim();
    }

    private static String safeCsv(Object value) {
        String text = Objects.toString(value, "");
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }

    private static final class CsvColumns {
        private final Map<String, Integer> indexByHeader;
        private final int leadingOffset;

        private CsvColumns(Map<String, Integer> indexByHeader, int leadingOffset) {
            this.indexByHeader = indexByHeader;
            this.leadingOffset = leadingOffset;
        }

        private static CsvColumns fromHeader(String headerLine) {
            String[] cells = splitCsv(headerLine);
            Map<String, Integer> indexByHeader = new LinkedHashMap<>();
            for (int i = 0; i < cells.length; i++) {
                String key = clean(cells[i]).toLowerCase(Locale.ROOT);
                if (!key.isBlank()) {
                    indexByHeader.putIfAbsent(key, i);
                }
            }
            int leadingOffset = indexByHeader.getOrDefault("id", -1) == 0 ? 1 : 0;
            return new CsvColumns(indexByHeader, leadingOffset);
        }
    }
}

