package com.jivs.platform.service.migration;

import com.jivs.platform.domain.migration.ValidationRule;
import com.jivs.platform.domain.quality.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for validating migration data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    /**
     * Validate migration data
     */
    public ValidationResult validateMigrationData(ValidationContext context) {
        log.info("Validating migration data for migration: {}", context.getMigrationId());

        ValidationResult result = new ValidationResult();
        result.setMigrationId(context.getMigrationId());
        result.setStartTime(new Date());

        List<ValidationError> errors = new ArrayList<>();
        List<ValidationWarning> warnings = new ArrayList<>();

        // Execute validation rules
        for (ValidationRule rule : context.getValidationRules()) {
            try {
                ValidationOutcome outcome = executeRule(rule, context);

                if (outcome.hasErrors()) {
                    errors.addAll(outcome.getErrors());
                }
                if (outcome.hasWarnings()) {
                    warnings.addAll(outcome.getWarnings());
                }
            } catch (Exception e) {
                log.error("Failed to execute validation rule: {}", rule.getName(), e);
                errors.add(new ValidationError(
                    rule.getName(),
                    "Rule execution failed: " + e.getMessage(),
                    Severity.CRITICAL
                ));
            }
        }

        // Set results
        result.setErrors(errors);
        result.setWarnings(warnings);
        result.setErrorCount(errors.size());
        result.setWarningCount(warnings.size());

        // Calculate validation score
        double score = calculateValidationScore(errors, warnings, context.getValidationRules().size());
        result.setScore(score);

        // Determine if passed
        boolean passed = errors.stream()
            .noneMatch(e -> e.getSeverity() == Severity.CRITICAL || e.getSeverity() == Severity.MAJOR);
        result.setPassed(passed);

        // Generate summary
        result.setSummary(generateSummary(errors, warnings));

        result.setEndTime(new Date());
        result.setDuration(result.getEndTime().getTime() - result.getStartTime().getTime());

        log.info("Validation completed. Score: {}%, Passed: {}, Errors: {}, Warnings: {}",
            score, passed, errors.size(), warnings.size());

        return result;
    }

    /**
     * Execute a single validation rule
     */
    private ValidationOutcome executeRule(ValidationRule rule, ValidationContext context) {
        ValidationOutcome outcome = new ValidationOutcome();

        switch (rule.getRuleType()) {
            case "SCHEMA":
                validateSchema(rule, context, outcome);
                break;
            case "DATA_TYPE":
                validateDataTypes(rule, context, outcome);
                break;
            case "CONSTRAINTS":
                validateConstraints(rule, context, outcome);
                break;
            case "REFERENTIAL":
                validateReferentialIntegrity(rule, context, outcome);
                break;
            case "BUSINESS":
                validateBusinessRules(rule, context, outcome);
                break;
            case "COMPLETENESS":
                validateCompleteness(rule, context, outcome);
                break;
            case "UNIQUENESS":
                validateUniqueness(rule, context, outcome);
                break;
            case "FORMAT":
                validateFormat(rule, context, outcome);
                break;
            default:
                log.warn("Unknown validation rule type: {}", rule.getRuleType());
        }

        return outcome;
    }

    /**
     * Validate schema compatibility
     */
    private void validateSchema(ValidationRule rule, ValidationContext context, ValidationOutcome outcome) {
        // Compare source and target schemas
        Map<String, FieldSchema> sourceSchema = context.getSourceSchema();
        Map<String, FieldSchema> targetSchema = context.getTargetSchema();

        for (Map.Entry<String, FieldSchema> entry : targetSchema.entrySet()) {
            String field = entry.getKey();
            FieldSchema targetField = entry.getValue();

            if (targetField.isRequired() && !sourceSchema.containsKey(field)) {
                outcome.addError(new ValidationError(
                    field,
                    "Required field missing in source",
                    Severity.CRITICAL
                ));
            }

            if (sourceSchema.containsKey(field)) {
                FieldSchema sourceField = sourceSchema.get(field);

                // Check type compatibility
                if (!isTypeCompatible(sourceField.getDataType(), targetField.getDataType())) {
                    outcome.addError(new ValidationError(
                        field,
                        String.format("Incompatible types: source=%s, target=%s",
                            sourceField.getDataType(), targetField.getDataType()),
                        Severity.MAJOR
                    ));
                }

                // Check length constraints
                if (targetField.getMaxLength() != null &&
                    sourceField.getMaxLength() != null &&
                    sourceField.getMaxLength() > targetField.getMaxLength()) {
                    outcome.addWarning(new ValidationWarning(
                        field,
                        String.format("Source max length (%d) exceeds target (%d)",
                            sourceField.getMaxLength(), targetField.getMaxLength())
                    ));
                }
            }
        }
    }

    /**
     * Validate data types
     */
    private void validateDataTypes(ValidationRule rule, ValidationContext context, ValidationOutcome outcome) {
        Map<String, List<Object>> sampleData = context.getSampleData();

        for (Map.Entry<String, List<Object>> entry : sampleData.entrySet()) {
            String field = entry.getKey();
            List<Object> values = entry.getValue();

            FieldSchema schema = context.getTargetSchema().get(field);
            if (schema != null) {
                for (Object value : values) {
                    if (!isValidDataType(value, schema.getDataType())) {
                        outcome.addError(new ValidationError(
                            field,
                            String.format("Invalid data type. Expected: %s, Got: %s",
                                schema.getDataType(), value.getClass().getSimpleName()),
                            Severity.MAJOR
                        ));
                        break; // Report once per field
                    }
                }
            }
        }
    }

    /**
     * Validate constraints
     */
    private void validateConstraints(ValidationRule rule, ValidationContext context, ValidationOutcome outcome) {
        Map<String, List<Object>> sampleData = context.getSampleData();

        for (Map.Entry<String, List<Object>> entry : sampleData.entrySet()) {
            String field = entry.getKey();
            List<Object> values = entry.getValue();

            FieldSchema schema = context.getTargetSchema().get(field);
            if (schema != null && schema.getConstraints() != null) {
                for (FieldConstraint constraint : schema.getConstraints()) {
                    validateConstraint(field, values, constraint, outcome);
                }
            }
        }
    }

    /**
     * Validate single constraint
     */
    private void validateConstraint(
            String field,
            List<Object> values,
            FieldConstraint constraint,
            ValidationOutcome outcome) {

        switch (constraint.getType()) {
            case "NOT_NULL":
                if (values.stream().anyMatch(Objects::isNull)) {
                    outcome.addError(new ValidationError(
                        field,
                        "Null values found in NOT NULL field",
                        Severity.CRITICAL
                    ));
                }
                break;

            case "MIN_VALUE":
                Double min = constraint.getMinValue();
                if (min != null) {
                    for (Object value : values) {
                        if (value instanceof Number &&
                            ((Number) value).doubleValue() < min) {
                            outcome.addError(new ValidationError(
                                field,
                                String.format("Value below minimum: %s < %s", value, min),
                                Severity.MAJOR
                            ));
                            break;
                        }
                    }
                }
                break;

            case "MAX_VALUE":
                Double max = constraint.getMaxValue();
                if (max != null) {
                    for (Object value : values) {
                        if (value instanceof Number &&
                            ((Number) value).doubleValue() > max) {
                            outcome.addError(new ValidationError(
                                field,
                                String.format("Value exceeds maximum: %s > %s", value, max),
                                Severity.MAJOR
                            ));
                            break;
                        }
                    }
                }
                break;

            case "PATTERN":
                String pattern = constraint.getPattern();
                if (pattern != null) {
                    for (Object value : values) {
                        if (value != null && !value.toString().matches(pattern)) {
                            outcome.addWarning(new ValidationWarning(
                                field,
                                String.format("Value doesn't match pattern: %s", pattern)
                            ));
                            break;
                        }
                    }
                }
                break;
        }
    }

    /**
     * Validate referential integrity
     */
    private void validateReferentialIntegrity(
            ValidationRule rule,
            ValidationContext context,
            ValidationOutcome outcome) {

        Map<String, ReferenceInfo> references = context.getReferences();

        for (Map.Entry<String, ReferenceInfo> entry : references.entrySet()) {
            String field = entry.getKey();
            ReferenceInfo ref = entry.getValue();

            // Check if all referenced values exist
            Set<Object> missingReferences = findMissingReferences(ref);

            if (!missingReferences.isEmpty()) {
                outcome.addError(new ValidationError(
                    field,
                    String.format("Missing references: %s", missingReferences),
                    Severity.CRITICAL
                ));
            }
        }
    }

    /**
     * Validate business rules
     */
    private void validateBusinessRules(
            ValidationRule rule,
            ValidationContext context,
            ValidationOutcome outcome) {

        List<BusinessRule> businessRules = context.getBusinessRules();

        for (BusinessRule businessRule : businessRules) {
            try {
                boolean passed = evaluateBusinessRule(businessRule, context.getSampleData());

                if (!passed) {
                    Severity severity = businessRule.isCritical() ? Severity.CRITICAL : Severity.MAJOR;
                    outcome.addError(new ValidationError(
                        businessRule.getName(),
                        businessRule.getDescription(),
                        severity
                    ));
                }
            } catch (Exception e) {
                outcome.addWarning(new ValidationWarning(
                    businessRule.getName(),
                    "Failed to evaluate rule: " + e.getMessage()
                ));
            }
        }
    }

    /**
     * Validate data completeness
     */
    private void validateCompleteness(
            ValidationRule rule,
            ValidationContext context,
            ValidationOutcome outcome) {

        Map<String, List<Object>> sampleData = context.getSampleData();
        double threshold = rule.getCompletenessThreshold() != null ? rule.getCompletenessThreshold() : 0.95;

        for (Map.Entry<String, List<Object>> entry : sampleData.entrySet()) {
            String field = entry.getKey();
            List<Object> values = entry.getValue();

            long nonNullCount = values.stream().filter(Objects::nonNull).count();
            double completeness = (double) nonNullCount / values.size();

            if (completeness < threshold) {
                outcome.addWarning(new ValidationWarning(
                    field,
                    String.format("Low completeness: %.2f%% (threshold: %.2f%%)",
                        completeness * 100, threshold * 100)
                ));
            }
        }
    }

    /**
     * Validate uniqueness constraints
     */
    private void validateUniqueness(
            ValidationRule rule,
            ValidationContext context,
            ValidationOutcome outcome) {

        Map<String, List<Object>> sampleData = context.getSampleData();
        Set<String> uniqueFields = context.getUniqueFields();

        for (String field : uniqueFields) {
            List<Object> values = sampleData.get(field);
            if (values != null) {
                Set<Object> uniqueValues = new HashSet<>(values);

                if (uniqueValues.size() < values.size()) {
                    outcome.addError(new ValidationError(
                        field,
                        String.format("Duplicate values found. Unique: %d, Total: %d",
                            uniqueValues.size(), values.size()),
                        Severity.CRITICAL
                    ));
                }
            }
        }
    }

    /**
     * Validate data format
     */
    private void validateFormat(
            ValidationRule rule,
            ValidationContext context,
            ValidationOutcome outcome) {

        Map<String, String> formatPatterns = context.getFormatPatterns();

        for (Map.Entry<String, String> entry : formatPatterns.entrySet()) {
            String field = entry.getKey();
            String pattern = entry.getValue();

            List<Object> values = context.getSampleData().get(field);
            if (values != null) {
                long invalidCount = values.stream()
                    .filter(Objects::nonNull)
                    .filter(v -> !v.toString().matches(pattern))
                    .count();

                if (invalidCount > 0) {
                    outcome.addWarning(new ValidationWarning(
                        field,
                        String.format("Invalid format found in %d values", invalidCount)
                    ));
                }
            }
        }
    }

    // Helper methods
    private boolean isTypeCompatible(String sourceType, String targetType) {
        // Check if types are compatible for conversion
        if (sourceType.equals(targetType)) {
            return true;
        }

        // Check common compatible conversions
        Map<String, Set<String>> compatibilityMap = new HashMap<>();
        compatibilityMap.put("INTEGER", Set.of("LONG", "DOUBLE", "DECIMAL", "STRING"));
        compatibilityMap.put("LONG", Set.of("DOUBLE", "DECIMAL", "STRING"));
        compatibilityMap.put("FLOAT", Set.of("DOUBLE", "DECIMAL", "STRING"));
        compatibilityMap.put("DOUBLE", Set.of("DECIMAL", "STRING"));

        Set<String> compatible = compatibilityMap.get(sourceType);
        return compatible != null && compatible.contains(targetType);
    }

    private boolean isValidDataType(Object value, String expectedType) {
        if (value == null) {
            return true; // Null is valid for all types unless NOT NULL constraint
        }

        switch (expectedType.toUpperCase()) {
            case "STRING":
                return value instanceof String;
            case "INTEGER":
                return value instanceof Integer;
            case "LONG":
                return value instanceof Long || value instanceof Integer;
            case "DOUBLE":
                return value instanceof Double || value instanceof Float || value instanceof Number;
            case "BOOLEAN":
                return value instanceof Boolean;
            case "DATE":
            case "DATETIME":
                return value instanceof Date || value instanceof java.time.temporal.Temporal;
            default:
                return true; // Unknown type, assume valid
        }
    }

    private Set<Object> findMissingReferences(ReferenceInfo ref) {
        Set<Object> sourceValues = new HashSet<>(ref.getSourceValues());
        Set<Object> targetValues = new HashSet<>(ref.getTargetValues());
        sourceValues.removeAll(targetValues);
        return sourceValues;
    }

    private boolean evaluateBusinessRule(BusinessRule rule, Map<String, List<Object>> data) {
        // Evaluate business rule expression
        // This would use a rule engine or expression evaluator
        return true; // Simplified
    }

    private double calculateValidationScore(
            List<ValidationError> errors,
            List<ValidationWarning> warnings,
            int totalRules) {

        if (totalRules == 0) {
            return 100.0;
        }

        double criticalPenalty = errors.stream()
            .filter(e -> e.getSeverity() == Severity.CRITICAL)
            .count() * 20;

        double majorPenalty = errors.stream()
            .filter(e -> e.getSeverity() == Severity.MAJOR)
            .count() * 10;

        double minorPenalty = errors.stream()
            .filter(e -> e.getSeverity() == Severity.MINOR)
            .count() * 5;

        double warningPenalty = warnings.size() * 2;

        double score = 100.0 - criticalPenalty - majorPenalty - minorPenalty - warningPenalty;
        return Math.max(0, Math.min(100, score));
    }

    private String generateSummary(List<ValidationError> errors, List<ValidationWarning> warnings) {
        StringBuilder summary = new StringBuilder();

        if (errors.isEmpty() && warnings.isEmpty()) {
            summary.append("Validation passed with no issues.");
        } else {
            summary.append("Validation completed with ");

            if (!errors.isEmpty()) {
                Map<Severity, Long> errorsBySeverity = errors.stream()
                    .collect(Collectors.groupingBy(
                        ValidationError::getSeverity,
                        Collectors.counting()
                    ));

                summary.append(errors.size()).append(" error(s): ");
                errorsBySeverity.forEach((severity, count) ->
                    summary.append(count).append(" ").append(severity).append(", ")
                );
            }

            if (!warnings.isEmpty()) {
                summary.append(warnings.size()).append(" warning(s)");
            }
        }

        return summary.toString();
    }
}

// Supporting classes
class ValidationContext {
    private Long migrationId;
    private String sourceSystem;
    private String targetSystem;
    private List<ValidationRule> validationRules;
    private Map<String, FieldSchema> sourceSchema;
    private Map<String, FieldSchema> targetSchema;
    private Map<String, List<Object>> sampleData;
    private Map<String, ReferenceInfo> references;
    private List<BusinessRule> businessRules;
    private Set<String> uniqueFields;
    private Map<String, String> formatPatterns;

    // Getters and setters
    public Long getMigrationId() { return migrationId; }
    public void setMigrationId(Long migrationId) { this.migrationId = migrationId; }
    public String getSourceSystem() { return sourceSystem; }
    public void setSourceSystem(String sourceSystem) { this.sourceSystem = sourceSystem; }
    public String getTargetSystem() { return targetSystem; }
    public void setTargetSystem(String targetSystem) { this.targetSystem = targetSystem; }
    public List<ValidationRule> getValidationRules() { return validationRules; }
    public void setValidationRules(List<ValidationRule> validationRules) {
        this.validationRules = validationRules;
    }
    public Map<String, FieldSchema> getSourceSchema() { return sourceSchema; }
    public Map<String, FieldSchema> getTargetSchema() { return targetSchema; }
    public Map<String, List<Object>> getSampleData() { return sampleData; }
    public Map<String, ReferenceInfo> getReferences() { return references; }
    public List<BusinessRule> getBusinessRules() { return businessRules; }
    public Set<String> getUniqueFields() { return uniqueFields; }
    public Map<String, String> getFormatPatterns() { return formatPatterns; }
}

class ValidationResult {
    private Long migrationId;
    private boolean passed;
    private double score;
    private int errorCount;
    private int warningCount;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;
    private String summary;
    private Date startTime;
    private Date endTime;
    private long duration;

    // Getters and setters
    public Long getMigrationId() { return migrationId; }
    public void setMigrationId(Long migrationId) { this.migrationId = migrationId; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public int getErrorCount() { return errorCount; }
    public void setErrorCount(int errorCount) { this.errorCount = errorCount; }
    public int getWarningCount() { return warningCount; }
    public void setWarningCount(int warningCount) { this.warningCount = warningCount; }
    public List<ValidationError> getErrors() { return errors; }
    public void setErrors(List<ValidationError> errors) { this.errors = errors; }
    public List<ValidationWarning> getWarnings() { return warnings; }
    public void setWarnings(List<ValidationWarning> warnings) { this.warnings = warnings; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
}

class ValidationOutcome {
    private List<ValidationError> errors = new ArrayList<>();
    private List<ValidationWarning> warnings = new ArrayList<>();

    public void addError(ValidationError error) { errors.add(error); }
    public void addWarning(ValidationWarning warning) { warnings.add(warning); }
    public boolean hasErrors() { return !errors.isEmpty(); }
    public boolean hasWarnings() { return !warnings.isEmpty(); }
    public List<ValidationError> getErrors() { return errors; }
    public List<ValidationWarning> getWarnings() { return warnings; }
}

class ValidationError {
    private String field;
    private String message;
    private Severity severity;

    public ValidationError(String field, String message, Severity severity) {
        this.field = field;
        this.message = message;
        this.severity = severity;
    }

    public String getField() { return field; }
    public String getMessage() { return message; }
    public Severity getSeverity() { return severity; }
}

class ValidationWarning {
    private String field;
    private String message;

    public ValidationWarning(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() { return field; }
    public String getMessage() { return message; }
}

class FieldSchema {
    private String name;
    private String dataType;
    private boolean required;
    private Integer maxLength;
    private List<FieldConstraint> constraints;

    // Getters and setters
    public String getName() { return name; }
    public String getDataType() { return dataType; }
    public boolean isRequired() { return required; }
    public Integer getMaxLength() { return maxLength; }
    public List<FieldConstraint> getConstraints() { return constraints; }
}

class FieldConstraint {
    private String type;
    private Double minValue;
    private Double maxValue;
    private String pattern;

    // Getters and setters
    public String getType() { return type; }
    public Double getMinValue() { return minValue; }
    public Double getMaxValue() { return maxValue; }
    public String getPattern() { return pattern; }
}

class ReferenceInfo {
    private String sourceField;
    private String targetTable;
    private String targetField;
    private List<Object> sourceValues;
    private List<Object> targetValues;

    // Getters and setters
    public List<Object> getSourceValues() { return sourceValues; }
    public List<Object> getTargetValues() { return targetValues; }
}

class BusinessRule {
    private String name;
    private String description;
    private String expression;
    private boolean critical;

    // Getters and setters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getExpression() { return expression; }
    public boolean isCritical() { return critical; }
}