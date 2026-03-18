// This DTO defines response payload fields for Api endpoints.
package com.chesscoach.main.dto.common;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Setter
@Getter
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private List<ApiError> errors;
    private OffsetDateTime timestamp;
    private String path;

    public ApiResponse() {
    }

    public static <T> ApiResponse<T> ok(String message, T data, String path) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(OffsetDateTime.now());
        response.setPath(path);
        return response;
    }

    public static <T> ApiResponse<T> created(String message, T data, String path) {
        return ok(message, data, path);
    }

    public static <T> ApiResponse<T> fail(String message, List<ApiError> errors, String path) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(false);
        response.setMessage(message);
        response.setErrors(errors);
        response.setTimestamp(OffsetDateTime.now());
        response.setPath(path);
        return response;
    }

}

