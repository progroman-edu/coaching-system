package com.chesscoach.main.exception;

import com.chesscoach.main.dto.common.ApiError;
import com.chesscoach.main.dto.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        List<ApiError> errors = List.of(new ApiError("NOT_FOUND", ex.getMessage()));
        ApiResponse<Void> response = ApiResponse.fail("Resource not found", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ConflictException ex, HttpServletRequest request) {
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
                return new ApiError("VALIDATION_ERROR", field + ": " + error.getDefaultMessage());
            })
            .toList();

        ApiResponse<Void> response = ApiResponse.fail("Validation failed", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler({MaxUploadSizeExceededException.class, MultipartException.class})
    public ResponseEntity<ApiResponse<Void>> handleMultipart(Exception ex, HttpServletRequest request) {
        List<ApiError> errors = List.of(new ApiError("UPLOAD_ERROR", ex.getMessage()));
        ApiResponse<Void> response = ApiResponse.fail("Invalid file upload", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex, HttpServletRequest request) {
        List<ApiError> errors = List.of(new ApiError("INTERNAL_ERROR", ex.getMessage()));
        ApiResponse<Void> response = ApiResponse.fail("Unexpected server error", errors, request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
