// This file converts application exceptions into consistent API error responses.
package com.chesscoach.main.exception;

import com.chesscoach.main.dto.common.ApiError;
import com.chesscoach.main.dto.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        String details = String.format("Resource not found: %s (Path: %s)", ex.getMessage(), request.getRequestURI());
        log.warn(details);
        List<ApiError> errors = List.of(new ApiError("NOT_FOUND", ex.getMessage()));
        ApiResponse<Void> response = ApiResponse.fail("Resource not found", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex, HttpServletRequest request) {
        String details = String.format("Conflict detected: %s (Path: %s)", ex.getMessage(), request.getRequestURI());
        log.warn(details);
        List<ApiError> errors = List.of(new ApiError("CONFLICT", ex.getMessage()));
        ApiResponse<Void> response = ApiResponse.fail("Conflict", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError> errors = ex.getBindingResult()
            .getAllErrors()
            .stream()
            .map(error -> {
                String field = error instanceof FieldError fieldError ? fieldError.getField() : "request";
                String message = String.format("%s: %s (Expected: %s)", 
                    field, error.getDefaultMessage(), error.getObjectName());
                return new ApiError("VALIDATION_ERROR", message);
            })
            .toList();

        String details = String.format("Validation failed for %d field(s) at %s", errors.size(), request.getRequestURI());
        log.warn(details);
        ApiResponse<Void> response = ApiResponse.fail("Validation failed", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMultipart(Exception ex, HttpServletRequest request) {
        String details = String.format("Upload error at %s: %s", request.getRequestURI(), ex.getMessage());
        log.warn(details);
        List<ApiError> errors = List.of(new ApiError("UPLOAD_ERROR", "Invalid upload payload: " + ex.getMessage()));
        ApiResponse<Void> response = ApiResponse.fail("Invalid file upload", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        String details = String.format("Bad request at %s: %s", request.getRequestURI(), ex.getMessage());
        log.warn(details);
        List<ApiError> errors = List.of(new ApiError("BAD_REQUEST", ex.getMessage()));
        ApiResponse<Void> response = ApiResponse.fail("Invalid request", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        String details = String.format("Resource not found: %s", request.getRequestURI());
        log.warn(details);
        List<ApiError> errors = List.of(new ApiError("NOT_FOUND", "Resource not found: " + request.getRequestURI()));
        ApiResponse<Void> response = ApiResponse.fail("Resource not found", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex, HttpServletRequest request) {
        String path = request.getRequestURI();
        // Only return JSON error for API endpoints (/api/...)
        if (path.startsWith("/api/")) {
            String details = String.format("No handler found for %s %s", ex.getHttpMethod(), path);
            log.warn(details);
            List<ApiError> errors = List.of(new ApiError("NOT_FOUND", "Endpoint not found: " + ex.getHttpMethod() + " " + path));
            ApiResponse<Void> response = ApiResponse.fail("Endpoint not found", errors, path);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        // For non-API paths, return 404 without body to let Spring handle it (serves index.html or default)
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        String details = String.format("Unhandled server error at %s: %s (%s)", 
            request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage());
        log.error(details, ex);
        List<ApiError> errors = List.of(new ApiError("INTERNAL_ERROR", "An unexpected error occurred"));
        ApiResponse<Void> response = ApiResponse.fail("Unexpected server error", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}

