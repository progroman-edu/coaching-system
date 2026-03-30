// This service interface defines operations for Trainee workflows.
package com.chesscoach.main.service;

import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface TraineeService {
    TraineeResponse create(TraineeRequest request);

    List<TraineeResponse> list(
        String search,
        Integer ratingMin,
        String department,
        String rankingOrder,
        Integer page,
        Integer size
    );

    TraineeResponse getById(Long id);

    TraineeResponse update(Long id, TraineeRequest request);

    TraineeResponse updatePhoto(Long id, MultipartFile file);

    void delete(Long id);

    void resetTraineeTestData();
}

