package com.jivs.platform.service.migration;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the result of a migration validation operation.
 * Used to track validation status, errors, warnings, and quality scores.
 */
@Data
@Builder
public class ValidationResult {

    /**
     * Indicates whether the validation passed successfully
     */
    private boolean passed;

    /**
     * List of validation errors encountered
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * List of validation warnings (non-critical issues)
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    /**
     * Validation quality score (0.0 - 1.0)
     */
    private double score;

    /**
     * Migration phase where validation occurred
     */
    private String phase;

    /**
     * Check if there are any validation errors
     * @return true if errors list is not empty
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Check if there are any validation warnings
     * @return true if warnings list is not empty
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }

    /**
     * Add a validation error
     * @param error the error message to add
     */
    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
        this.passed = false;
    }

    /**
     * Add a validation warning
     * @param warning the warning message to add
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
}
