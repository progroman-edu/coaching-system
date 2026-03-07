// This service implementation contains business logic for Trainee operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;
import com.chesscoach.main.dto.chesscom.ChessComRatingResponse;
import com.chesscoach.main.exception.ResourceNotFoundException;
import com.chesscoach.main.model.Coach;
import com.chesscoach.main.model.Trainee;
import com.chesscoach.main.repository.CoachRepository;
import com.chesscoach.main.repository.TraineeRepository;
import com.chesscoach.main.service.ChessComService;
import com.chesscoach.main.service.ImageStorageService;
import com.chesscoach.main.service.TraineeService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class TraineeServiceImpl implements TraineeService {

    private static final String DEFAULT_COACH_EMAIL = "coach@local";

    private final TraineeRepository traineeRepository;
    private final CoachRepository coachRepository;
    private final ImageStorageService imageStorageService;
    private final ChessComService chessComService;

    public TraineeServiceImpl(
        TraineeRepository traineeRepository,
        CoachRepository coachRepository,
        ImageStorageService imageStorageService,
        ChessComService chessComService
    ) {
        this.traineeRepository = traineeRepository;
        this.coachRepository = coachRepository;
        this.imageStorageService = imageStorageService;
        this.chessComService = chessComService;
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
        Integer ratingMin,
        Integer ratingMax,
        Integer ageMin,
        Integer ageMax,
        String courseStrand,
        Integer page,
        Integer size
    ) {
        return traineeRepository.search(
            ratingMin,
            ratingMax,
            ageMin,
            ageMax,
            courseStrand,
            PageRequest.of(page, size)
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
        if (!traineeRepository.existsById(id)) {
            throw new ResourceNotFoundException("Trainee not found: " + id);
        }
        traineeRepository.deleteById(id);
        recomputeRankings();
    }

    private Trainee getTraineeOrThrow(Long id) {
        return traineeRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Trainee not found: " + id));
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

    private void applyRequest(Trainee trainee, TraineeRequest request) {
        trainee.setName(request.getName());
        trainee.setAge(request.getAge());
        trainee.setAddress(request.getAddress());
        trainee.setGradeLevel(request.getGradeLevel());
        trainee.setCourseStrand(request.getCourseStrand());
        trainee.setPhotoPath(request.getPhotoPath());
        trainee.setChessUsername(request.getChessUsername());
    }

    private void applyRatingsFromChessCom(Trainee trainee, String chessUsername) {
        ChessComRatingResponse ratings = chessComService.getRatings(chessUsername);
        Integer current = ratings.getRapid();
        if (current == null) {
            current = ratings.getBlitz();
        }
        if (current == null) {
            current = ratings.getBullet();
        }
        if (current == null) {
            throw new IllegalStateException("No rapid/blitz/bullet rating found for Chess.com user: " + chessUsername);
        }
        trainee.setCurrentRating(current);
        trainee.setHighestRating(current);
    }

    private void recomputeRankings() {
        List<Trainee> leaderboard = traineeRepository.findAllByOrderByCurrentRatingDescIdAsc();
        int rank = 1;
        for (Trainee t : leaderboard) {
            t.setRanking(rank++);
        }
        traineeRepository.saveAll(leaderboard);
    }

    private TraineeResponse toResponse(Trainee trainee) {
        TraineeResponse response = new TraineeResponse();
        response.setId(trainee.getId());
        response.setName(trainee.getName());
        response.setAge(trainee.getAge());
        response.setAddress(trainee.getAddress());
        response.setGradeLevel(trainee.getGradeLevel());
        response.setCourseStrand(trainee.getCourseStrand());
        response.setCurrentRating(trainee.getCurrentRating());
        response.setHighestRating(trainee.getHighestRating());
        response.setRanking(trainee.getRanking());
        response.setPhotoPath(trainee.getPhotoPath());
        response.setChessUsername(trainee.getChessUsername());
        return response;
    }
}

