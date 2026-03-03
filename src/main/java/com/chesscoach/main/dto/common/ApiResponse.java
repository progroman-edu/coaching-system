// This file contains project logic for ApiResponse.
package com.chesscoach.main.dto.common;

import java.time.OffsetDateTime;
import java.util.List;

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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public List<ApiError> getErrors() {
        return errors;
    }

    public void setErrors(List<ApiError> errors) {
        this.errors = errors;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}

