package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;
import com.chesscoach.main.service.TraineeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping(ApiPaths.TRAINEES)
public class TraineeController {

    private final TraineeService traineeService;

    public TraineeController(TraineeService traineeService) {
        this.traineeService = traineeService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TraineeResponse>> create(
        @Valid @RequestBody TraineeRequest requestBody,
        HttpServletRequest request
    ) {
        TraineeResponse data = traineeService.create(requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("Trainee created", data, request.getRequestURI()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TraineeResponse>>> list(
        @RequestParam(required = false) Integer ratingMin,
        @RequestParam(required = false) Integer ratingMax,
        @RequestParam(required = false) Integer ageMin,
        @RequestParam(required = false) Integer ageMax,
        @RequestParam(required = false) String courseStrand,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        HttpServletRequest request
    ) {
        List<TraineeResponse> data = traineeService.list(
            ratingMin,
            ratingMax,
            ageMin,
            ageMax,
            courseStrand,
            page,
            size
        );
        return ResponseEntity.ok(ApiResponse.ok("Trainee list", data, request.getRequestURI()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TraineeResponse>> getById(
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        TraineeResponse data = traineeService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Trainee detail", data, request.getRequestURI()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TraineeResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody TraineeRequest requestBody,
        HttpServletRequest request
    ) {
        TraineeResponse data = traineeService.update(id, requestBody);
        return ResponseEntity.ok(ApiResponse.ok("Trainee updated", data, request.getRequestURI()));
    }

    @PostMapping(value = "/{id}/photo", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<TraineeResponse>> uploadPhoto(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        TraineeResponse data = traineeService.updatePhoto(id, file);
        return ResponseEntity.ok(ApiResponse.ok("Trainee photo uploaded", data, request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        traineeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Trainee deleted: " + id, null, request.getRequestURI()));
    }
}
