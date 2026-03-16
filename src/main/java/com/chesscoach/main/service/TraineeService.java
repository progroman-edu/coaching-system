// This service interface defines operations for Trainee workflows.
package com.chesscoach.main.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;

public interface TraineeService {
    TraineeResponse create(TraineeRequest request);

    List<TraineeResponse> list(
        Integer ratingMin,
        String rankingOrder,
        Integer size
    );

    TraineeResponse getById(Long id);

    TraineeResponse update(Long id, TraineeRequest request);

    TraineeResponse updatePhoto(Long id, MultipartFile file);

    void delete(Long id);

    void resetTraineeTestData();
}

