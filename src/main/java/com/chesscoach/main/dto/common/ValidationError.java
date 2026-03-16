// This DTO defines a shared API data contract for ValidationError.
package com.chesscoach.main.dto.common;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ValidationError {
    private String field;
    private String message;

    public ValidationError() {
    }

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

}

