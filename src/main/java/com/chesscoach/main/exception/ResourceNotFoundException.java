// This file defines a domain exception for missing resources.
package com.chesscoach.main.exception;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

