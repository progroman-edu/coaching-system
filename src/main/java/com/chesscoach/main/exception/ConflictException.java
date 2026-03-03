// This file defines a domain exception for conflicting operations.
package com.chesscoach.main.exception;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

