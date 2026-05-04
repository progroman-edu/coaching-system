// This service implementation contains business logic for Trainee operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;
import com.chesscoach.main.dto.chesscom.ChessComRatingResponse;
import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.BlitzRating;
import com.chesscoach.main.model.BulletRating;
import com.chesscoach.main.model.Coach;
import com.chesscoach.main.model.RapidRating;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.AttendanceRepository;
import com.chesscoach.main.repository.BlitzRatingRepository;
import com.chesscoach.main.repository.BulletRatingRepository;
import com.chesscoach.main.repository.CoachRepository;
import com.chesscoach.main.repository.MatchParticipantRepository;
import com.chesscoach.main.repository.MatchRepository;
import com.chesscoach.main.repository.MatchResultRepository;
import com.chesscoach.main.repository.NotificationRepository;
import com.chesscoach.main.repository.RapidRatingRepository;
import com.chesscoach.main.repository.RatingsHistoryRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.ChessComService;
import com.chesscoach.main.service.ImageStorageService;
import com.chesscoach.main.service.TraineeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class TraineeServiceImpl implements TraineeService {

    private static final Logger log = LoggerFactory.getLogger(TraineeServiceImpl.class);

    private final TraineeRepository traineeRepository;
    private final CoachRepository coachRepository;
    private final ImageStorageService imageStorageService;
    private final ChessComService chessComService;
    private final AttendanceRepository attendanceRepository;
    private final MatchResultRepository matchResultRepository;
    private final MatchParticipantRepository matchParticipantRepository;
    private final MatchRepository matchRepository;
    private final RatingsHistoryRepository ratingsHistoryRepository;
    private final NotificationRepository notificationRepository;
    private final BlitzRatingRepository blitzRatingRepository;
    private final BulletRatingRepository bulletRatingRepository;
    private final RapidRatingRepository rapidRatingRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.rating.default:1200}")
    private int defaultRating;

    @Value("${app.coach.default-email}")
    private String defaultCoachEmail;

    @Value("${app.coach.default-name}")
    private String defaultCoachName;

    @Value("${app.coach.default-phone}")
    private String defaultCoachPhone;

    public TraineeServiceImpl(
        TraineeRepository traineeRepository,
        CoachRepository coachRepository,
        ImageStorageService imageStorageService,
        ChessComService chessComService,
        AttendanceRepository attendanceRepository,
        MatchResultRepository matchResultRepository,
        MatchParticipantRepository matchParticipantRepository,
        MatchRepository matchRepository,
        RatingsHistoryRepository ratingsHistoryRepository,
        NotificationRepository notificationRepository,
        BlitzRatingRepository blitzRatingRepository,
        BulletRatingRepository bulletRatingRepository,
        RapidRatingRepository rapidRatingRepository,
        JdbcTemplate jdbcTemplate
    ) {
        this.traineeRepository = traineeRepository;
        this.coachRepository = coachRepository;
        this.imageStorageService = imageStorageService;
        this.chessComService = chessComService;
        this.attendanceRepository = attendanceRepository;
        this.matchResultRepository = matchResultRepository;
        this.matchParticipantRepository = matchParticipantRepository;
        this.matchRepository = matchRepository;
        this.ratingsHistoryRepository = ratingsHistoryRepository;
        this.notificationRepository = notificationRepository;
        this.blitzRatingRepository = blitzRatingRepository;
        this.bulletRatingRepository = bulletRatingRepository;
        this.rapidRatingRepository = rapidRatingRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public TraineeResponse create(TraineeRequest request) {
        Trainee trainee = new Trainee();
        trainee.setCoach(getOrCreateDefaultCoach());
        applyRequest(trainee, request);
        applyRatingsFromChessCom(trainee, request.getChessUsername());
        Trainee saved = traineeRepository.save(trainee);
        recomputeRankings();
        return toResponse(getTraineeOrThrow(saved.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraineeResponse> list(
        String search,
        Integer ratingMin,
        String department,
        String rankingOrder,
        Integer page,
        Integer size
    ) {
        Sort sort = resolveRankingSort(rankingOrder);
        return traineeRepository.search(
            normalizeSearch(search),
            ratingMin,
            department,
            PageRequest.of(page, size, sort)
        ).map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TraineeResponse getById(Long id) {
        return toResponse(getTraineeOrThrow(id));
    }

    @Override
    @Transactional
    public TraineeResponse update(Long id, TraineeRequest request) {
        Trainee trainee = getTraineeOrThrow(id);
        applyRequest(trainee, request);
        applyRatingsFromChessCom(trainee, request.getChessUsername());
        return toResponse(traineeRepository.save(trainee));
    }

    @Override
    @Transactional
    public TraineeResponse updatePhoto(Long id, MultipartFile file) {
        Trainee trainee = getTraineeOrThrow(id);
        String photoPath = imageStorageService.saveTraineePhoto(id, file);
        trainee.setPhotoPath(photoPath);
        return toResponse(traineeRepository.save(trainee));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Trainee trainee = getTraineeOrThrow(id);
        if (trainee.getDeletedAt() != null) {
            throw new ResourceNotFoundException("Trainee already deleted: " + id);
        }
        // Soft delete: mark with deletedAt timestamp instead of hard delete
        trainee.setDeletedAt(OffsetDateTime.now());
        traineeRepository.save(trainee);
        recomputeRankings();
        log.info("Trainee soft-deleted: {} at {}", id, trainee.getDeletedAt());
    }

    @Override
    @Transactional
    public void resetTraineeTestData() {
        ratingsHistoryRepository.deleteAllInBatch();
        notificationRepository.deleteAllInBatch();
        attendanceRepository.deleteAllInBatch();
        matchResultRepository.deleteAllInBatch();
        matchParticipantRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        rapidRatingRepository.deleteAllInBatch();
        blitzRatingRepository.deleteAllInBatch();
        bulletRatingRepository.deleteAllInBatch();
        traineeRepository.deleteAllInBatch();
        resetAutoIncrementIfNoTrainees();
    }

    @Override
    @Transactional
    public void resetMatchesOnly() {
        ratingsHistoryRepository.deleteAllInBatch();
        matchResultRepository.deleteAllInBatch();
        matchParticipantRepository.deleteAllInBatch();
        matchRepository.deleteAllInBatch();
        log.info("Match records reset complete (trainees preserved)");
    }

    private Trainee getTraineeOrThrow(Long id) {
        return traineeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + id));
    }

    private Coach getOrCreateDefaultCoach() {
        return coachRepository.findByEmail(defaultCoachEmail)
            .orElseGet(() -> {
                Coach coach = new Coach();
                coach.setFullName(defaultCoachName);
                coach.setEmail(defaultCoachEmail);
                coach.setPhone(defaultCoachPhone);
                return coachRepository.save(coach);
            });
    }

    private void applyRequest(Trainee trainee, TraineeRequest request) {
        trainee.setName(request.getName());
        trainee.setAge(request.getAge());
        trainee.setAddress(request.getAddress());
        trainee.setGradeLevel(request.getGradeLevel());
        trainee.setDepartment(request.getDepartment());
        trainee.setPhotoPath(validatePhotoPath(request.getPhotoPath()));
        trainee.setChessUsername(normalizeUsername(request.getChessUsername()));
    }

    private String validatePhotoPath(String photoPath) {
        if (photoPath == null || photoPath.trim().isEmpty()) {
            return null;
        }
        
        String normalized = photoPath.trim();
        
        // Security: Prevent directory traversal attacks
        if (normalized.contains("..") || normalized.contains("\\") || normalized.startsWith("/")) {
            throw new IllegalArgumentException("Invalid photo path: path traversal detected");
        }
        
        // Only allow expected photo paths from the configured upload directory
        if (!normalized.startsWith("uploads/") && !normalized.startsWith("trainee_photos/")) {
            throw new IllegalArgumentException("Invalid photo path: must be in uploads or trainee_photos directory");
        }
        
        return normalized;
    }

    private void applyRatingsFromChessCom(Trainee trainee, String chessUsername) {
        String normalizedUsername = normalizeUsername(chessUsername);
        
        // Create rating entities for all game modes
        BlitzRating blitzRating = new BlitzRating();
        blitzRating.setTrainee(trainee);
        
        BulletRating bulletRating = new BulletRating();
        bulletRating.setTrainee(trainee);
        
        RapidRating rapidRating = new RapidRating();
        rapidRating.setTrainee(trainee);
        
        if (normalizedUsername == null) {
            blitzRating.setCurrentRating(defaultRating);
            blitzRating.setHighestRating(defaultRating);
            bulletRating.setCurrentRating(defaultRating);
            bulletRating.setHighestRating(defaultRating);
            rapidRating.setCurrentRating(defaultRating);
            rapidRating.setHighestRating(defaultRating);
        } else {
            ChessComRatingResponse ratings = chessComService.getRatings(normalizedUsername);
            Integer rapid = ratings.getRapid();
            Integer rapidBest = ratings.getRapidBest();
            Integer blitz = ratings.getBlitz();
            Integer blitzBest = ratings.getBlitzBest();
            Integer bullet = ratings.getBullet();
            Integer bulletBest = ratings.getBulletBest();
            
            // Set current and best ratings separately
            rapidRating.setCurrentRating(rapid != null ? rapid : defaultRating);
            rapidRating.setHighestRating(rapidBest != null ? rapidBest : (rapid != null ? rapid : defaultRating));
            
            blitzRating.setCurrentRating(blitz != null ? blitz : defaultRating);
            blitzRating.setHighestRating(blitzBest != null ? blitzBest : (blitz != null ? blitz : defaultRating));
            
            bulletRating.setCurrentRating(bullet != null ? bullet : defaultRating);
            bulletRating.setHighestRating(bulletBest != null ? bulletBest : (bullet != null ? bullet : defaultRating));
        }
        
        trainee.setBlitzRating(blitzRating);
        trainee.setBulletRating(bulletRating);
        trainee.setRapidRating(rapidRating);
    }

    private static String normalizeUsername(String username) {
        if (username == null) {
            return null;
        }
        String normalized = username.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeSearch(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static Sort resolveRankingSort(String rankingOrder) {
        String normalized = rankingOrder == null ? "asc" : rankingOrder.trim().toLowerCase();
        return switch (normalized) {
            case "desc" -> Sort.by(Sort.Order.desc("ranking"), Sort.Order.asc("id"));
            case "asc", "" -> Sort.by(Sort.Order.asc("ranking"), Sort.Order.asc("id"));
            default -> throw new IllegalArgumentException("rankingOrder must be 'asc' or 'desc'");
        };
    }

    private void recomputeRankings() {
        List<Trainee> trainees = traineeRepository.findAll();
        trainees.sort((a, b) -> {
            int ratingA = getRapidCurrentRating(a);
            int ratingB = getRapidCurrentRating(b);
            if (ratingA != ratingB) {
                return Integer.compare(ratingB, ratingA); // descending
            }
            return Long.compare(a.getId(), b.getId()); // ascending by id
        });
        int rank = 1;
        for (Trainee t : trainees) {
            t.setRanking(rank++);
        }
        traineeRepository.saveAll(trainees);
    }

    @SuppressWarnings("UnboxingNullableValue")
    private int getRapidCurrentRating(Trainee trainee) {
        RapidRating rapid = trainee.getRapidRating();
        Integer current = rapid != null && rapid.getCurrentRating() != null ? rapid.getCurrentRating() : defaultRating;
        return current != null ? current : defaultRating;
    }

    private void resetAutoIncrementIfNoTrainees() {
        if (traineeRepository.count() > 0) {
            return;
        }
        try {
            jdbcTemplate.execute("ALTER TABLE trainees AUTO_INCREMENT = 1");
        } catch (DataAccessException ex) {
            log.debug("Could not reset trainees AUTO_INCREMENT on current database engine: {}", ex.getMessage());
        }
    }

    @SuppressWarnings("UnboxingNullableValue")
    private TraineeResponse toResponse(Trainee trainee) {
        TraineeResponse response = new TraineeResponse();
        response.setId(trainee.getId());
        response.setName(trainee.getName());
        response.setAge(trainee.getAge());
        response.setAddress(trainee.getAddress());
        response.setGradeLevel(trainee.getGradeLevel());
        response.setDepartment(trainee.getDepartment());
        
        // Map ratings from separate tables
        BlitzRating blitz = trainee.getBlitzRating();
        BulletRating bullet = trainee.getBulletRating();
        RapidRating rapid = trainee.getRapidRating();
        
        Integer blitzCurrent = blitz != null && blitz.getCurrentRating() != null ? blitz.getCurrentRating() : defaultRating;
        Integer blitzHighest = blitz != null && blitz.getHighestRating() != null ? blitz.getHighestRating() : defaultRating;
        Integer bulletCurrent = bullet != null && bullet.getCurrentRating() != null ? bullet.getCurrentRating() : defaultRating;
        Integer bulletHighest = bullet != null && bullet.getHighestRating() != null ? bullet.getHighestRating() : defaultRating;
        Integer rapidCurrent = rapid != null && rapid.getCurrentRating() != null ? rapid.getCurrentRating() : defaultRating;
        Integer rapidHighest = rapid != null && rapid.getHighestRating() != null ? rapid.getHighestRating() : defaultRating;
        response.setBlitzCurrentRating(blitzCurrent);
        response.setBlitzHighestRating(blitzHighest);
        response.setBulletCurrentRating(bulletCurrent);
        response.setBulletHighestRating(bulletHighest);
        response.setRapidCurrentRating(rapidCurrent);
        response.setRapidHighestRating(rapidHighest);
        
        response.setLatestRatingChange(findLatestRatingChange(trainee.getId()));
        response.setAttendancePercentageLast30Days(computeAttendancePercentageLast30Days(trainee.getId()));
        response.setLastActivityAt(resolveLastActivityAt(trainee));
        response.setRanking(trainee.getRanking());
        response.setPhotoPath(trainee.getPhotoPath());
        response.setChessUsername(trainee.getChessUsername());
        return response;
    }

    private Integer findLatestRatingChange(Long traineeId) {
        List<com.chesscoach.main.model.RatingsHistory> history = ratingsHistoryRepository.findByTraineeIdOrderByCreatedAtDesc(traineeId);
        if (history.isEmpty()) {
            return 0;
        }
        Integer change = history.getFirst().getRatingChange();
        return change != null ? change : 0;
    }

    private Double computeAttendancePercentageLast30Days(Long traineeId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);
        List<com.chesscoach.main.model.Attendance> records =
            attendanceRepository.findByTraineeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(traineeId, startDate, endDate);
        if (records.isEmpty()) {
            return 0.0;
        }
        long present = records.stream().filter(record -> Boolean.TRUE.equals(record.getPresent())).count();
        return (present * 100.0) / records.size();
    }

    private OffsetDateTime resolveLastActivityAt(Trainee trainee) {
        OffsetDateTime latest = trainee.getUpdatedAt() != null ? trainee.getUpdatedAt() : OffsetDateTime.now();
        List<com.chesscoach.main.model.MatchResult> results =
            matchResultRepository.findByWhiteTraineeIdOrBlackTraineeIdOrderByPlayedAtDesc(trainee.getId(), trainee.getId());
        if (!results.isEmpty()) {
            OffsetDateTime playedAt = results.getFirst().getPlayedAt();
            if (playedAt != null && playedAt.isAfter(latest)) {
                latest = playedAt;
            }
        }
        return latest;
    }
}

