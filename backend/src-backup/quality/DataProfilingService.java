package com.jivs.platform.service.quality;

import com.jivs.platform.domain.quality.DataProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for data profiling and statistical analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataProfilingService {

    /**
     * Profile data to generate statistics and patterns
     */
    public DataProfile profile(Map<String, Object> data) {
        log.debug("Profiling data with {} fields", data.size());

        DataProfile profile = new DataProfile();
        profile.setProfileDate(LocalDateTime.now());
        profile.setRecordCount(calculateRecordCount(data));

        // Analyze each field
        Map<String, FieldProfile> fieldProfiles = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            FieldProfile fieldProfile = profileField(entry.getKey(), entry.getValue());
            fieldProfiles.put(entry.getKey(), fieldProfile);
        }

        profile.setFieldProfiles(fieldProfiles);

        // Calculate overall statistics
        profile.setCompleteness(calculateCompleteness(fieldProfiles));
        profile.setUniqueness(calculateUniqueness(fieldProfiles));
        profile.setPatternCompliance(calculatePatternCompliance(fieldProfiles));

        // Identify data patterns
        profile.setPatterns(identifyPatterns(data));

        // Detect data distributions
        profile.setDistributions(analyzeDistributions(fieldProfiles));

        return profile;
    }

    /**
     * Profile individual field
     */
    private FieldProfile profileField(String fieldName, Object value) {
        FieldProfile profile = new FieldProfile();
        profile.setFieldName(fieldName);

        if (value == null) {
            profile.setNullCount(1);
            profile.setCompleteness(0.0);
            return profile;
        }

        profile.setDataType(detectDataType(value));

        if (value instanceof Collection) {
            profileCollection(profile, (Collection<?>) value);
        } else if (value instanceof Map) {
            profileMap(profile, (Map<?, ?>) value);
        } else {
            profileSingleValue(profile, value);
        }

        return profile;
    }

    /**
     * Profile collection field
     */
    private void profileCollection(FieldProfile profile, Collection<?> collection) {
        profile.setCount(collection.size());

        if (collection.isEmpty()) {
            profile.setEmptyCount(1);
            return;
        }

        // Analyze collection elements
        Set<Object> uniqueValues = new HashSet<>(collection);
        profile.setUniqueCount(uniqueValues.size());
        profile.setCardinality((double) uniqueValues.size() / collection.size());

        // Get sample values
        profile.setSampleValues(
            collection.stream()
                .limit(10)
                .map(Object::toString)
                .collect(Collectors.toList())
        );

        // Analyze numeric collections
        if (isNumericCollection(collection)) {
            analyzeNumericCollection(profile, collection);
        }

        // Analyze string collections
        if (isStringCollection(collection)) {
            analyzeStringCollection(profile, collection);
        }
    }

    /**
     * Analyze numeric collection
     */
    private void analyzeNumericCollection(FieldProfile profile, Collection<?> collection) {
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (Object value : collection) {
            if (value instanceof Number) {
                stats.addValue(((Number) value).doubleValue());
            }
        }

        NumericStatistics numStats = new NumericStatistics();
        numStats.setMin(stats.getMin());
        numStats.setMax(stats.getMax());
        numStats.setMean(stats.getMean());
        numStats.setMedian(stats.getPercentile(50));
        numStats.setStandardDeviation(stats.getStandardDeviation());
        numStats.setVariance(stats.getVariance());

        // Calculate percentiles
        Map<Integer, Double> percentiles = new HashMap<>();
        percentiles.put(25, stats.getPercentile(25));
        percentiles.put(50, stats.getPercentile(50));
        percentiles.put(75, stats.getPercentile(75));
        percentiles.put(90, stats.getPercentile(90));
        percentiles.put(95, stats.getPercentile(95));
        percentiles.put(99, stats.getPercentile(99));
        numStats.setPercentiles(percentiles);

        profile.setNumericStatistics(numStats);
    }

    /**
     * Analyze string collection
     */
    private void analyzeStringCollection(FieldProfile profile, Collection<?> collection) {
        StringStatistics strStats = new StringStatistics();

        List<String> strings = collection.stream()
            .filter(v -> v != null)
            .map(Object::toString)
            .collect(Collectors.toList());

        if (!strings.isEmpty()) {
            // Length statistics
            IntSummaryStatistics lengthStats = strings.stream()
                .mapToInt(String::length)
                .summaryStatistics();

            strStats.setMinLength(lengthStats.getMin());
            strStats.setMaxLength(lengthStats.getMax());
            strStats.setAvgLength(lengthStats.getAverage());

            // Pattern detection
            Map<String, Integer> patterns = detectStringPatterns(strings);
            strStats.setPatterns(patterns);

            // Common prefixes/suffixes
            strStats.setCommonPrefixes(findCommonPrefixes(strings));
            strStats.setCommonSuffixes(findCommonSuffixes(strings));
        }

        profile.setStringStatistics(strStats);
    }

    /**
     * Detect string patterns
     */
    private Map<String, Integer> detectStringPatterns(List<String> strings) {
        Map<String, Integer> patterns = new HashMap<>();

        for (String str : strings) {
            String pattern = derivePattern(str);
            patterns.merge(pattern, 1, Integer::sum);
        }

        // Keep only top patterns
        return patterns.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(10)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }

    /**
     * Derive pattern from string
     */
    private String derivePattern(String str) {
        return str.replaceAll("[0-9]", "N")
            .replaceAll("[a-z]", "a")
            .replaceAll("[A-Z]", "A")
            .replaceAll("[^NaA]", "X");
    }

    /**
     * Find common prefixes
     */
    private List<String> findCommonPrefixes(List<String> strings) {
        Map<String, Integer> prefixCount = new HashMap<>();

        for (String str : strings) {
            for (int i = 1; i <= Math.min(5, str.length()); i++) {
                String prefix = str.substring(0, i);
                prefixCount.merge(prefix, 1, Integer::sum);
            }
        }

        return prefixCount.entrySet().stream()
            .filter(e -> e.getValue() > strings.size() / 10) // At least 10% occurrence
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Find common suffixes
     */
    private List<String> findCommonSuffixes(List<String> strings) {
        Map<String, Integer> suffixCount = new HashMap<>();

        for (String str : strings) {
            for (int i = 1; i <= Math.min(5, str.length()); i++) {
                String suffix = str.substring(str.length() - i);
                suffixCount.merge(suffix, 1, Integer::sum);
            }
        }

        return suffixCount.entrySet().stream()
            .filter(e -> e.getValue() > strings.size() / 10) // At least 10% occurrence
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(5)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Profile single value
     */
    private void profileSingleValue(FieldProfile profile, Object value) {
        profile.setCount(1);
        profile.setUniqueCount(1);
        profile.setCardinality(1.0);
        profile.setSampleValues(Collections.singletonList(value.toString()));

        if (value instanceof Number) {
            NumericStatistics stats = new NumericStatistics();
            double numValue = ((Number) value).doubleValue();
            stats.setMin(numValue);
            stats.setMax(numValue);
            stats.setMean(numValue);
            stats.setMedian(numValue);
            profile.setNumericStatistics(stats);
        } else if (value instanceof String) {
            StringStatistics stats = new StringStatistics();
            String strValue = (String) value;
            stats.setMinLength(strValue.length());
            stats.setMaxLength(strValue.length());
            stats.setAvgLength(strValue.length());
            profile.setStringStatistics(stats);
        }
    }

    /**
     * Profile map field
     */
    private void profileMap(FieldProfile profile, Map<?, ?> map) {
        profile.setCount(map.size());
        profile.setNestedStructure(true);

        // Analyze nested structure
        Map<String, FieldProfile> nestedProfiles = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            FieldProfile nestedProfile = profileField(key, entry.getValue());
            nestedProfiles.put(key, nestedProfile);
        }
        profile.setNestedFields(nestedProfiles);
    }

    /**
     * Identify data patterns
     */
    private List<DataPattern> identifyPatterns(Map<String, Object> data) {
        List<DataPattern> patterns = new ArrayList<>();

        // Identify key patterns
        patterns.addAll(identifyKeyPatterns(data.keySet()));

        // Identify value patterns
        patterns.addAll(identifyValuePatterns(data.values()));

        // Identify relationship patterns
        patterns.addAll(identifyRelationshipPatterns(data));

        return patterns;
    }

    /**
     * Analyze data distributions
     */
    private Map<String, Distribution> analyzeDistributions(Map<String, FieldProfile> fieldProfiles) {
        Map<String, Distribution> distributions = new HashMap<>();

        for (Map.Entry<String, FieldProfile> entry : fieldProfiles.entrySet()) {
            FieldProfile profile = entry.getValue();

            if (profile.getNumericStatistics() != null) {
                Distribution dist = analyzeNumericDistribution(profile.getNumericStatistics());
                distributions.put(entry.getKey(), dist);
            } else if (profile.getStringStatistics() != null) {
                Distribution dist = analyzeStringDistribution(profile.getStringStatistics());
                distributions.put(entry.getKey(), dist);
            }
        }

        return distributions;
    }

    /**
     * Helper methods
     */
    private int calculateRecordCount(Map<String, Object> data) {
        // Calculate number of records in data
        return 1; // Simplified - treating as single record
    }

    private double calculateCompleteness(Map<String, FieldProfile> profiles) {
        if (profiles.isEmpty()) return 0.0;

        double totalCompleteness = profiles.values().stream()
            .mapToDouble(FieldProfile::getCompleteness)
            .sum();

        return totalCompleteness / profiles.size();
    }

    private double calculateUniqueness(Map<String, FieldProfile> profiles) {
        if (profiles.isEmpty()) return 0.0;

        double totalCardinality = profiles.values().stream()
            .mapToDouble(FieldProfile::getCardinality)
            .sum();

        return totalCardinality / profiles.size();
    }

    private double calculatePatternCompliance(Map<String, FieldProfile> profiles) {
        // Calculate overall pattern compliance
        return 0.85; // Simplified
    }

    private String detectDataType(Object value) {
        if (value instanceof Number) return "NUMERIC";
        if (value instanceof String) return "STRING";
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof LocalDateTime) return "DATETIME";
        if (value instanceof Collection) return "COLLECTION";
        if (value instanceof Map) return "OBJECT";
        return "UNKNOWN";
    }

    private boolean isNumericCollection(Collection<?> collection) {
        return collection.stream().allMatch(v -> v instanceof Number);
    }

    private boolean isStringCollection(Collection<?> collection) {
        return collection.stream().allMatch(v -> v instanceof String);
    }

    private List<DataPattern> identifyKeyPatterns(Set<String> keys) {
        List<DataPattern> patterns = new ArrayList<>();
        // Identify patterns in field names
        return patterns;
    }

    private List<DataPattern> identifyValuePatterns(Collection<Object> values) {
        List<DataPattern> patterns = new ArrayList<>();
        // Identify patterns in values
        return patterns;
    }

    private List<DataPattern> identifyRelationshipPatterns(Map<String, Object> data) {
        List<DataPattern> patterns = new ArrayList<>();
        // Identify relationships between fields
        return patterns;
    }

    private Distribution analyzeNumericDistribution(NumericStatistics stats) {
        Distribution dist = new Distribution();
        dist.setType("NUMERIC");
        // Analyze distribution characteristics
        return dist;
    }

    private Distribution analyzeStringDistribution(StringStatistics stats) {
        Distribution dist = new Distribution();
        dist.setType("STRING");
        // Analyze distribution characteristics
        return dist;
    }
}

/**
 * Field profile
 */
class FieldProfile {
    private String fieldName;
    private String dataType;
    private int count;
    private int nullCount;
    private int emptyCount;
    private int uniqueCount;
    private double completeness = 1.0;
    private double cardinality;
    private List<String> sampleValues;
    private NumericStatistics numericStatistics;
    private StringStatistics stringStatistics;
    private boolean nestedStructure;
    private Map<String, FieldProfile> nestedFields;

    // Getters and setters
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
    public int getNullCount() { return nullCount; }
    public void setNullCount(int nullCount) { this.nullCount = nullCount; }
    public int getEmptyCount() { return emptyCount; }
    public void setEmptyCount(int emptyCount) { this.emptyCount = emptyCount; }
    public int getUniqueCount() { return uniqueCount; }
    public void setUniqueCount(int uniqueCount) { this.uniqueCount = uniqueCount; }
    public double getCompleteness() { return completeness; }
    public void setCompleteness(double completeness) { this.completeness = completeness; }
    public double getCardinality() { return cardinality; }
    public void setCardinality(double cardinality) { this.cardinality = cardinality; }
    public List<String> getSampleValues() { return sampleValues; }
    public void setSampleValues(List<String> sampleValues) { this.sampleValues = sampleValues; }
    public NumericStatistics getNumericStatistics() { return numericStatistics; }
    public void setNumericStatistics(NumericStatistics numericStatistics) {
        this.numericStatistics = numericStatistics;
    }
    public StringStatistics getStringStatistics() { return stringStatistics; }
    public void setStringStatistics(StringStatistics stringStatistics) {
        this.stringStatistics = stringStatistics;
    }
    public boolean isNestedStructure() { return nestedStructure; }
    public void setNestedStructure(boolean nestedStructure) {
        this.nestedStructure = nestedStructure;
    }
    public Map<String, FieldProfile> getNestedFields() { return nestedFields; }
    public void setNestedFields(Map<String, FieldProfile> nestedFields) {
        this.nestedFields = nestedFields;
    }
}

/**
 * Numeric statistics
 */
class NumericStatistics {
    private double min;
    private double max;
    private double mean;
    private double median;
    private double standardDeviation;
    private double variance;
    private Map<Integer, Double> percentiles;

    // Getters and setters
    public double getMin() { return min; }
    public void setMin(double min) { this.min = min; }
    public double getMax() { return max; }
    public void setMax(double max) { this.max = max; }
    public double getMean() { return mean; }
    public void setMean(double mean) { this.mean = mean; }
    public double getMedian() { return median; }
    public void setMedian(double median) { this.median = median; }
    public double getStandardDeviation() { return standardDeviation; }
    public void setStandardDeviation(double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }
    public double getVariance() { return variance; }
    public void setVariance(double variance) { this.variance = variance; }
    public Map<Integer, Double> getPercentiles() { return percentiles; }
    public void setPercentiles(Map<Integer, Double> percentiles) {
        this.percentiles = percentiles;
    }
}

/**
 * String statistics
 */
class StringStatistics {
    private int minLength;
    private int maxLength;
    private double avgLength;
    private Map<String, Integer> patterns;
    private List<String> commonPrefixes;
    private List<String> commonSuffixes;

    // Getters and setters
    public int getMinLength() { return minLength; }
    public void setMinLength(int minLength) { this.minLength = minLength; }
    public int getMaxLength() { return maxLength; }
    public void setMaxLength(int maxLength) { this.maxLength = maxLength; }
    public double getAvgLength() { return avgLength; }
    public void setAvgLength(double avgLength) { this.avgLength = avgLength; }
    public Map<String, Integer> getPatterns() { return patterns; }
    public void setPatterns(Map<String, Integer> patterns) { this.patterns = patterns; }
    public List<String> getCommonPrefixes() { return commonPrefixes; }
    public void setCommonPrefixes(List<String> commonPrefixes) {
        this.commonPrefixes = commonPrefixes;
    }
    public List<String> getCommonSuffixes() { return commonSuffixes; }
    public void setCommonSuffixes(List<String> commonSuffixes) {
        this.commonSuffixes = commonSuffixes;
    }
}

/**
 * Data pattern
 */
class DataPattern {
    private String patternType;
    private String description;
    private double confidence;

    // Constructor and getters/setters
}

/**
 * Distribution
 */
class Distribution {
    private String type;
    private Map<String, Object> parameters;

    // Getters and setters
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}