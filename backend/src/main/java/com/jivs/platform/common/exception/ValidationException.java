package com.jivs.platform.common.exception;

import java.util.Map;

/**
 * Exception thrown for validation errors
 */
public class ValidationException extends RuntimeException {

    private final Map<String, String> errors;

    public ValidationException(String message) {
        super(message);
        this.errors = null;
    }

    public ValidationException(String message, Map<String, String> errors) {
        super(message);
        this.errors = errors;
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = null;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
