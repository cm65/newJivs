package com.jivs.platform.service.quality;

import com.jivs.platform.domain.quality.DataAnomaly;
import com.jivs.platform.domain.quality.AnomalyType;
import com.jivs.platform.domain.quality.Severity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting anomalies in data
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyDetectionService {

    private static final double Z_SCORE_THRESHOLD = 3.0;
    private static final double IQR_MULTIPLIER = 1.5;
    private static final int MIN_FREQUENCY_THRESHOLD = 5;

    /**
     * Detect anomalies in data
     */
    public List<DataAnomaly> detect(Map<String, Object> data) {
        log.debug("Detecting anomalies in data with {} fields", data.size());

        List<DataAnomaly> anomalies = new ArrayList<>();

        // Statistical anomalies
        anomalies.addAll(detectStatisticalAnomalies(data));

        // Pattern anomalies
        anomalies.addAll(detectPatternAnomalies(data));

        // Temporal anomalies
        anomalies.addAll(detectTemporalAnomalies(data));

        // Relational anomalies
        anomalies.addAll(detectRelationalAnomalies(data));

        // Business rule anomalies
        anomalies.addAll(detectBusinessRuleAnomalies(data));

        // Sort by severity
        anomalies.sort(Comparator.comparing(DataAnomaly::getSeverity).reversed());

        log.info("Detected {} anomalies in data", anomalies.size());
        return anomalies;
    }

    /**
     * Detect statistical anomalies using various methods
     */
    private List<DataAnomaly> detectStatisticalAnomalies(Map<String, Object> data) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Collection && isNumericCollection((Collection<?>) value)) {
                // Z-score method
                anomalies.addAll(detectZScoreAnomalies(field, (Collection<?>) value));

                // IQR method
                anomalies.addAll(detectIQRAnomalies(field, (Collection<?>) value));

                // Isolation Forest
                anomalies.addAll(detectIsolationForestAnomalies(field, (Collection<?>) value));
            } else if (value instanceof Number) {
                // Check single value anomalies
                DataAnomaly anomaly = checkSingleValueAnomaly(field, (Number) value);
                if (anomaly != null) {
                    anomalies.add(anomaly);
                }
            }
        }

        return anomalies;
    }

    /**
     * Detect anomalies using Z-score method
     */
    private List<DataAnomaly> detectZScoreAnomalies(String field, Collection<?> values) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        if (values.size() < 3) {
            return anomalies;
        }

        DescriptiveStatistics stats = new DescriptiveStatistics();
        List<Double> numericValues = new ArrayList<>();

        for (Object value : values) {
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                stats.addValue(numValue);
                numericValues.add(numValue);
            }
        }

        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();

        if (stdDev == 0) {
            return anomalies;
        }

        for (int i = 0; i < numericValues.size(); i++) {
            double value = numericValues.get(i);
            double zScore = Math.abs((value - mean) / stdDev);

            if (zScore > Z_SCORE_THRESHOLD) {
                DataAnomaly anomaly = new DataAnomaly();
                anomaly.setFieldName(field);
                anomaly.setAnomalyType(AnomalyType.OUTLIER);
                anomaly.setValue(value);
                anomaly.setScore(zScore);
                anomaly.setSeverity(calculateSeverity(zScore));
                anomaly.setDescription(String.format(
                    "Value %.2f is %.2f standard deviations from mean (%.2f)",
                    value, zScore, mean
                ));
                anomaly.setDetectionMethod("Z-Score");
                anomaly.setDetectionTime(LocalDateTime.now());
                anomaly.setIndex(i);
                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    /**
     * Detect anomalies using IQR method
     */
    private List<DataAnomaly> detectIQRAnomalies(String field, Collection<?> values) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        List<Double> sortedValues = values.stream()
            .filter(v -> v instanceof Number)
            .map(v -> ((Number) v).doubleValue())
            .sorted()
            .collect(Collectors.toList());

        if (sortedValues.size() < 4) {
            return anomalies;
        }

        int n = sortedValues.size();
        double q1 = sortedValues.get(n / 4);
        double q3 = sortedValues.get(3 * n / 4);
        double iqr = q3 - q1;

        double lowerBound = q1 - IQR_MULTIPLIER * iqr;
        double upperBound = q3 + IQR_MULTIPLIER * iqr;

        for (int i = 0; i < sortedValues.size(); i++) {
            double value = sortedValues.get(i);

            if (value < lowerBound || value > upperBound) {
                DataAnomaly anomaly = new DataAnomaly();
                anomaly.setFieldName(field);
                anomaly.setAnomalyType(AnomalyType.OUTLIER);
                anomaly.setValue(value);

                double distance = value < lowerBound ?
                    (lowerBound - value) / iqr :
                    (value - upperBound) / iqr;
                anomaly.setScore(distance);

                anomaly.setSeverity(calculateSeverity(distance));
                anomaly.setDescription(String.format(
                    "Value %.2f is outside IQR range [%.2f, %.2f]",
                    value, lowerBound, upperBound
                ));
                anomaly.setDetectionMethod("IQR");
                anomaly.setDetectionTime(LocalDateTime.now());
                anomaly.setIndex(i);
                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    /**
     * Detect anomalies using Isolation Forest concept
     */
    private List<DataAnomaly> detectIsolationForestAnomalies(String field, Collection<?> values) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        // Simplified isolation forest implementation
        List<Double> numericValues = values.stream()
            .filter(v -> v instanceof Number)
            .map(v -> ((Number) v).doubleValue())
            .collect(Collectors.toList());

        if (numericValues.size() < 10) {
            return anomalies;
        }

        // Calculate isolation scores
        for (int i = 0; i < numericValues.size(); i++) {
            double value = numericValues.get(i);
            double isolationScore = calculateIsolationScore(value, numericValues);

            if (isolationScore > 0.6) { // Threshold for anomaly
                DataAnomaly anomaly = new DataAnomaly();
                anomaly.setFieldName(field);
                anomaly.setAnomalyType(AnomalyType.ISOLATION);
                anomaly.setValue(value);
                anomaly.setScore(isolationScore);
                anomaly.setSeverity(calculateSeverity(isolationScore * 5)); // Scale for severity
                anomaly.setDescription(String.format(
                    "Value %.2f has high isolation score (%.2f)",
                    value, isolationScore
                ));
                anomaly.setDetectionMethod("Isolation Forest");
                anomaly.setDetectionTime(LocalDateTime.now());
                anomaly.setIndex(i);
                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    /**
     * Detect pattern-based anomalies
     */
    private List<DataAnomaly> detectPatternAnomalies(Map<String, Object> data) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                // Check format anomalies
                DataAnomaly formatAnomaly = checkFormatAnomaly(field, (String) value);
                if (formatAnomaly != null) {
                    anomalies.add(formatAnomaly);
                }

                // Check length anomalies
                DataAnomaly lengthAnomaly = checkLengthAnomaly(field, (String) value);
                if (lengthAnomaly != null) {
                    anomalies.add(lengthAnomaly);
                }
            } else if (value instanceof Collection && isStringCollection((Collection<?>) value)) {
                // Check pattern consistency
                anomalies.addAll(checkPatternConsistency(field, (Collection<?>) value));
            }
        }

        return anomalies;
    }

    /**
     * Detect temporal anomalies
     */
    private List<DataAnomaly> detectTemporalAnomalies(Map<String, Object> data) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof LocalDateTime) {
                // Check future dates
                DataAnomaly futureDateAnomaly = checkFutureDate(field, (LocalDateTime) value);
                if (futureDateAnomaly != null) {
                    anomalies.add(futureDateAnomaly);
                }

                // Check too old dates
                DataAnomaly oldDateAnomaly = checkOldDate(field, (LocalDateTime) value);
                if (oldDateAnomaly != null) {
                    anomalies.add(oldDateAnomaly);
                }
            } else if (value instanceof Collection && isDateCollection((Collection<?>) value)) {
                // Check temporal sequence anomalies
                anomalies.addAll(checkTemporalSequence(field, (Collection<?>) value));
            }
        }

        return anomalies;
    }

    /**
     * Detect relational anomalies
     */
    private List<DataAnomaly> detectRelationalAnomalies(Map<String, Object> data) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        // Check for missing relationships
        anomalies.addAll(checkMissingRelationships(data));

        // Check for invalid relationships
        anomalies.addAll(checkInvalidRelationships(data));

        // Check for circular dependencies
        anomalies.addAll(checkCircularDependencies(data));

        return anomalies;
    }

    /**
     * Detect business rule anomalies
     */
    private List<DataAnomaly> detectBusinessRuleAnomalies(Map<String, Object> data) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        // Check common business rules
        anomalies.addAll(checkCommonBusinessRules(data));

        // Check domain-specific rules
        anomalies.addAll(checkDomainSpecificRules(data));

        return anomalies;
    }

    /**
     * Check for format anomalies in string values
     */
    private DataAnomaly checkFormatAnomaly(String field, String value) {
        // Check for common format patterns
        Map<String, String> expectedPatterns = new HashMap<>();
        expectedPatterns.put("email", "^[A-Za-z0-9+_.-]+@(.+)$");
        expectedPatterns.put("phone", "^\\+?[1-9]\\d{1,14}$");
        expectedPatterns.put("url", "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
        expectedPatterns.put("ip", "^(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$");

        for (Map.Entry<String, String> pattern : expectedPatterns.entrySet()) {
            if (field.toLowerCase().contains(pattern.getKey())) {
                if (!value.matches(pattern.getValue())) {
                    DataAnomaly anomaly = new DataAnomaly();
                    anomaly.setFieldName(field);
                    anomaly.setAnomalyType(AnomalyType.FORMAT);
                    anomaly.setValue(value);
                    anomaly.setSeverity(Severity.MAJOR);
                    anomaly.setDescription("Value doesn't match expected " + pattern.getKey() + " format");
                    anomaly.setDetectionMethod("Pattern Matching");
                    anomaly.setDetectionTime(LocalDateTime.now());
                    return anomaly;
                }
            }
        }

        return null;
    }

    /**
     * Helper methods
     */
    private boolean isNumericCollection(Collection<?> collection) {
        return collection.stream().allMatch(v -> v instanceof Number);
    }

    private boolean isStringCollection(Collection<?> collection) {
        return collection.stream().allMatch(v -> v instanceof String);
    }

    private boolean isDateCollection(Collection<?> collection) {
        return collection.stream().allMatch(v -> v instanceof LocalDateTime);
    }

    private Severity calculateSeverity(double score) {
        if (score > 5) return Severity.CRITICAL;
        if (score > 3) return Severity.MAJOR;
        if (score > 1) return Severity.MINOR;
        return Severity.INFO;
    }

    private double calculateIsolationScore(double value, List<Double> allValues) {
        // Simplified isolation score calculation
        double min = allValues.stream().min(Double::compareTo).orElse(0.0);
        double max = allValues.stream().max(Double::compareTo).orElse(1.0);
        double range = max - min;

        if (range == 0) return 0.0;

        double normalizedValue = (value - min) / range;

        // Count how many splits would be needed to isolate this value
        int splits = 0;
        for (double other : allValues) {
            if (Math.abs(other - value) < range * 0.1) {
                splits++;
            }
        }

        // Lower splits mean easier to isolate (more anomalous)
        return 1.0 - (splits / (double) allValues.size());
    }

    private DataAnomaly checkSingleValueAnomaly(String field, Number value) {
        // Check for special numeric values
        if (value.doubleValue() == Double.POSITIVE_INFINITY ||
            value.doubleValue() == Double.NEGATIVE_INFINITY) {
            DataAnomaly anomaly = new DataAnomaly();
            anomaly.setFieldName(field);
            anomaly.setAnomalyType(AnomalyType.INVALID_VALUE);
            anomaly.setValue(value);
            anomaly.setSeverity(Severity.CRITICAL);
            anomaly.setDescription("Infinite value detected");
            anomaly.setDetectionTime(LocalDateTime.now());
            return anomaly;
        }

        if (Double.isNaN(value.doubleValue())) {
            DataAnomaly anomaly = new DataAnomaly();
            anomaly.setFieldName(field);
            anomaly.setAnomalyType(AnomalyType.INVALID_VALUE);
            anomaly.setValue(value);
            anomaly.setSeverity(Severity.CRITICAL);
            anomaly.setDescription("NaN value detected");
            anomaly.setDetectionTime(LocalDateTime.now());
            return anomaly;
        }

        return null;
    }

    private DataAnomaly checkLengthAnomaly(String field, String value) {
        // Check for unusually long or short strings
        if (value.length() > 1000) {
            DataAnomaly anomaly = new DataAnomaly();
            anomaly.setFieldName(field);
            anomaly.setAnomalyType(AnomalyType.LENGTH);
            anomaly.setValue(value.substring(0, 50) + "...");
            anomaly.setSeverity(Severity.MAJOR);
            anomaly.setDescription("Unusually long value (" + value.length() + " characters)");
            anomaly.setDetectionTime(LocalDateTime.now());
            return anomaly;
        }

        return null;
    }

    private List<DataAnomaly> checkPatternConsistency(String field, Collection<?> values) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        Map<String, Integer> patternFrequency = new HashMap<>();
        List<String> strings = values.stream()
            .map(Object::toString)
            .collect(Collectors.toList());

        for (String str : strings) {
            String pattern = derivePattern(str);
            patternFrequency.merge(pattern, 1, Integer::sum);
        }

        // Find rare patterns
        for (int i = 0; i < strings.size(); i++) {
            String str = strings.get(i);
            String pattern = derivePattern(str);
            int frequency = patternFrequency.get(pattern);

            if (frequency < MIN_FREQUENCY_THRESHOLD && strings.size() > 20) {
                DataAnomaly anomaly = new DataAnomaly();
                anomaly.setFieldName(field);
                anomaly.setAnomalyType(AnomalyType.PATTERN);
                anomaly.setValue(str);
                anomaly.setSeverity(Severity.MINOR);
                anomaly.setDescription("Rare pattern: " + pattern + " (frequency: " + frequency + ")");
                anomaly.setDetectionMethod("Pattern Analysis");
                anomaly.setDetectionTime(LocalDateTime.now());
                anomaly.setIndex(i);
                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    private String derivePattern(String str) {
        return str.replaceAll("[0-9]", "N")
            .replaceAll("[a-z]", "a")
            .replaceAll("[A-Z]", "A")
            .replaceAll("[^NaA]", "X");
    }

    private DataAnomaly checkFutureDate(String field, LocalDateTime date) {
        if (date.isAfter(LocalDateTime.now().plusDays(1))) {
            DataAnomaly anomaly = new DataAnomaly();
            anomaly.setFieldName(field);
            anomaly.setAnomalyType(AnomalyType.TEMPORAL);
            anomaly.setValue(date);
            anomaly.setSeverity(Severity.MAJOR);
            anomaly.setDescription("Future date detected: " + date);
            anomaly.setDetectionTime(LocalDateTime.now());
            return anomaly;
        }
        return null;
    }

    private DataAnomaly checkOldDate(String field, LocalDateTime date) {
        if (date.isBefore(LocalDateTime.now().minusYears(100))) {
            DataAnomaly anomaly = new DataAnomaly();
            anomaly.setFieldName(field);
            anomaly.setAnomalyType(AnomalyType.TEMPORAL);
            anomaly.setValue(date);
            anomaly.setSeverity(Severity.MAJOR);
            anomaly.setDescription("Unusually old date detected: " + date);
            anomaly.setDetectionTime(LocalDateTime.now());
            return anomaly;
        }
        return null;
    }

    private List<DataAnomaly> checkTemporalSequence(String field, Collection<?> values) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        List<LocalDateTime> dates = values.stream()
            .filter(v -> v instanceof LocalDateTime)
            .map(v -> (LocalDateTime) v)
            .sorted()
            .collect(Collectors.toList());

        for (int i = 1; i < dates.size(); i++) {
            long daysBetween = ChronoUnit.DAYS.between(dates.get(i-1), dates.get(i));

            if (daysBetween > 365) {
                DataAnomaly anomaly = new DataAnomaly();
                anomaly.setFieldName(field);
                anomaly.setAnomalyType(AnomalyType.TEMPORAL_GAP);
                anomaly.setValue(dates.get(i));
                anomaly.setSeverity(Severity.MINOR);
                anomaly.setDescription("Large temporal gap: " + daysBetween + " days");
                anomaly.setDetectionTime(LocalDateTime.now());
                anomaly.setIndex(i);
                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    private List<DataAnomaly> checkMissingRelationships(Map<String, Object> data) {
        // Check for expected relationships that are missing
        return new ArrayList<>(); // Simplified
    }

    private List<DataAnomaly> checkInvalidRelationships(Map<String, Object> data) {
        // Check for invalid foreign key references
        return new ArrayList<>(); // Simplified
    }

    private List<DataAnomaly> checkCircularDependencies(Map<String, Object> data) {
        // Check for circular references in data
        return new ArrayList<>(); // Simplified
    }

    private List<DataAnomaly> checkCommonBusinessRules(Map<String, Object> data) {
        List<DataAnomaly> anomalies = new ArrayList<>();

        // Check for negative prices
        Object price = data.get("price");
        if (price instanceof Number && ((Number) price).doubleValue() < 0) {
            DataAnomaly anomaly = new DataAnomaly();
            anomaly.setFieldName("price");
            anomaly.setAnomalyType(AnomalyType.BUSINESS_RULE);
            anomaly.setValue(price);
            anomaly.setSeverity(Severity.CRITICAL);
            anomaly.setDescription("Negative price detected");
            anomaly.setDetectionTime(LocalDateTime.now());
            anomalies.add(anomaly);
        }

        // Check for invalid percentages
        Object percentage = data.get("percentage");
        if (percentage instanceof Number) {
            double pct = ((Number) percentage).doubleValue();
            if (pct < 0 || pct > 100) {
                DataAnomaly anomaly = new DataAnomaly();
                anomaly.setFieldName("percentage");
                anomaly.setAnomalyType(AnomalyType.BUSINESS_RULE);
                anomaly.setValue(percentage);
                anomaly.setSeverity(Severity.MAJOR);
                anomaly.setDescription("Invalid percentage value: " + pct);
                anomaly.setDetectionTime(LocalDateTime.now());
                anomalies.add(anomaly);
            }
        }

        return anomalies;
    }

    private List<DataAnomaly> checkDomainSpecificRules(Map<String, Object> data) {
        // Check domain-specific business rules
        return new ArrayList<>(); // Simplified
    }
}