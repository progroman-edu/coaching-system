// This file contains project logic for ApiError.
package com.chesscoach.main.dto.common;

public class ApiError {
    private String code;
    private String detail;

    public ApiError() {
    }

    public ApiError(String code, String detail) {
        this.code = code;
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}

