package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.report.ReportExportResponse;
import com.chesscoach.main.dto.report.ReportImportResponse;
import com.chesscoach.main.model.Attendance;
import com.chesscoach.main.model.Coach;
import com.chesscoach.main.model.MatchResult;
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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class ReportServiceImpl implements ReportService {

    private static final String DEFAULT_COACH_EMAIL = "coach@local";

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

        Coach coach = getOrCreateDefaultCoach();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                totalRows++;
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                try {
                    upsertTraineeFromCsv(line, coach);
                    successRows++;
                } catch (Exception ex) {
                    failedRows++;
                }
            }
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to read import file: " + ex.getMessage());
        }

        ReportImportResponse response = new ReportImportResponse();
        response.setFileName(file.getOriginalFilename());
        response.setTotalRows(Math.max(0, totalRows - 1));
        response.setSuccessRows(successRows);
        response.setFailedRows(failedRows);
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
        lines.add("id,name,age,address,gradeLevel,courseStrand,currentRating,highestRating,ranking,photoPath");
        for (Trainee t : traineeRepository.findAll()) {
            lines.add(String.join(",",
                safeCsv(t.getId()),
                safeCsv(t.getName()),
                safeCsv(t.getAge()),
                safeCsv(t.getAddress()),
                safeCsv(t.getGradeLevel()),
                safeCsv(t.getCourseStrand()),
                safeCsv(t.getCurrentRating()),
                safeCsv(t.getHighestRating()),
                safeCsv(t.getRanking()),
                safeCsv(t.getPhotoPath())
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

    private void upsertTraineeFromCsv(String line, Coach coach) {
        String[] raw = splitCsv(line);
        if (raw.length < 6) {
            throw new IllegalArgumentException("Invalid CSV row");
        }

        String name = clean(raw[0]);
        Integer age = parseInt(raw[1], true);
        String address = clean(raw[2]);
        String gradeLevel = clean(raw[3]);
        String courseStrand = clean(raw[4]);
        Integer currentRating = parseInt(raw[5], true);
        Integer highestRating = raw.length > 6 ? parseInt(raw[6], false) : null;
        Integer ranking = raw.length > 7 ? parseInt(raw[7], false) : null;
        String photoPath = raw.length > 8 ? clean(raw[8]) : null;

        if (name.isBlank() || address.isBlank() || gradeLevel.isBlank() || courseStrand.isBlank()) {
            throw new IllegalArgumentException("Missing required trainee values");
        }

        Optional<Trainee> existing = traineeRepository.findByNameIgnoreCaseAndCoachId(name, coach.getId());
        Trainee trainee = existing.orElseGet(Trainee::new);
        trainee.setCoach(coach);
        trainee.setName(name);
        trainee.setAge(age);
        trainee.setAddress(address);
        trainee.setGradeLevel(gradeLevel);
        trainee.setCourseStrand(courseStrand);
        trainee.setCurrentRating(currentRating);
        trainee.setHighestRating(highestRating != null ? highestRating : currentRating);
        trainee.setRanking(ranking);
        trainee.setPhotoPath(photoPath);
        traineeRepository.save(trainee);
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
}
