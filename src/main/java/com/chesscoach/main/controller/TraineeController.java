// This controller exposes HTTP endpoints for Trainee workflows.
package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.dto.trainee.TraineeRequest;
import com.chesscoach.main.dto.trainee.TraineeResponse;
import com.chesscoach.main.service.TraineeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
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
@Tag(name = "Trainees", description = "APIs for managing trainee records, ratings, and photos")
public class TraineeController {

    private final TraineeService traineeService;
    private final boolean allowResetTestData;

    public TraineeController(
        TraineeService traineeService,
        @Value("${app.maintenance.allow-reset-test-data:false}") boolean allowResetTestData
    ) {
        this.traineeService = traineeService;
        this.allowResetTestData = allowResetTestData;
    }

    @PostMapping
    @Operation(summary = "Create a new trainee", description = "Create a new trainee with initial ratings of 1200")
    public ResponseEntity<ApiResponse<TraineeResponse>> create(
        @Valid @RequestBody TraineeRequest requestBody,
        HttpServletRequest request
    ) {
        TraineeResponse data = traineeService.create(requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created("Trainee created", data, request.getRequestURI()));
    }

    @GetMapping
    @Operation(summary = "List all trainees", description = "Get list of trainees with optional filtering by search, rating, and department")
    public ResponseEntity<ApiResponse<List<TraineeResponse>>> list(
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Integer ratingMin,
        @RequestParam(required = false) String department,
        @RequestParam(defaultValue = "asc") String rankingOrder,
        @RequestParam(defaultValue = "0") Integer page,
        @RequestParam(defaultValue = "20") Integer size,
        HttpServletRequest request
    ) {
        List<TraineeResponse> data = traineeService.list(
            search,
            ratingMin,
            department,
            rankingOrder,
            page,
            size
        );
        return ResponseEntity.ok(ApiResponse.ok("Trainee list", data, request.getRequestURI()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trainee by ID", description = "Retrieve detailed information about a specific trainee")
    public ResponseEntity<ApiResponse<TraineeResponse>> getById(
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        TraineeResponse data = traineeService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Trainee detail", data, request.getRequestURI()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update trainee information", description = "Update trainee details including name, age, ratings, and chess username")
    public ResponseEntity<ApiResponse<TraineeResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody TraineeRequest requestBody,
        HttpServletRequest request
    ) {
        TraineeResponse data = traineeService.update(id, requestBody);
        return ResponseEntity.ok(ApiResponse.ok("Trainee updated", data, request.getRequestURI()));
    }

    @PostMapping(value = "/{id}/photo", consumes = "multipart/form-data")
    @Operation(summary = "Upload trainee photo", description = "Upload or update trainee profile photo (max 5MB)")
    public ResponseEntity<ApiResponse<TraineeResponse>> uploadPhoto(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        TraineeResponse data = traineeService.updatePhoto(id, file);
        return ResponseEntity.ok(ApiResponse.ok("Trainee photo uploaded", data, request.getRequestURI()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a trainee", description = "Perform soft delete on a trainee record")
    public ResponseEntity<ApiResponse<Void>> delete(
        @PathVariable Long id,
        HttpServletRequest request
    ) {
        traineeService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Trainee deleted: " + id, null, request.getRequestURI()));
    }

    @DeleteMapping("/reset-test-data")
    @Operation(summary = "Reset test data", description = "Clear all trainee test data (development only)")
    public ResponseEntity<ApiResponse<Void>> resetTestData(
        @RequestParam(defaultValue = "false") boolean confirm,
        HttpServletRequest request
    ) {
        if (!allowResetTestData) {
            throw new IllegalStateException("reset-test-data endpoint is disabled");
        }
        if (!confirm) {
            throw new IllegalArgumentException("Set confirm=true to reset trainee test data");
        }
        traineeService.resetTraineeTestData();
        return ResponseEntity.ok(ApiResponse.ok("Trainee test data reset complete", null, request.getRequestURI()));
    }

    @DeleteMapping("/reset-matches")
    @Operation(summary = "Reset matches only", description = "Clear all match records while preserving trainees (development only)")
    public ResponseEntity<ApiResponse<Void>> resetMatches(
        @RequestParam(defaultValue = "false") boolean confirm,
        HttpServletRequest request
    ) {
        if (!allowResetTestData) {
            throw new IllegalStateException("reset-matches endpoint is disabled");
        }
        if (!confirm) {
            throw new IllegalArgumentException("Set confirm=true to reset match data");
        }
        traineeService.resetMatchesOnly();
        return ResponseEntity.ok(ApiResponse.ok("Match data reset complete (trainees preserved)", null, request.getRequestURI()));
    }
}

