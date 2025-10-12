package com.jivs.platform.service.transformation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jivs.platform.domain.transformation.TransformationRule;
import lombok.RequiredArgsConstructor;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core transformation engine for data format conversions
 */
@Component
@RequiredArgsConstructor
public class TransformationEngine {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TransformationEngine.class);

    private final ObjectMapper objectMapper;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();

    /**
     * Transform data based on rules and target format
     */
    public Map<String, Object> transform(
            Map<String, Object> sourceData,
            List<TransformationRule> rules,
            String targetFormat) {

        log.debug("Transforming data with {} rules to format: {}", rules.size(), targetFormat);

        // Apply transformation rules
        Map<String, Object> transformedData = applyRules(sourceData, rules);

        // Convert to target format
        transformedData = convertToTargetFormat(transformedData, targetFormat);

        return transformedData;
    }

    /**
     * Apply transformation rules to data
     */
    private Map<String, Object> applyRules(
            Map<String, Object> sourceData,
            List<TransformationRule> rules) {

        Map<String, Object> result = new HashMap<>(sourceData);

        // Sort rules by priority
        List<TransformationRule> sortedRules = new ArrayList<>(rules);
        sortedRules.sort(Comparator.comparingInt(TransformationRule::getPriority));

        for (TransformationRule rule : sortedRules) {
            try {
                result = applyRule(result, rule);
            } catch (Exception e) {
                log.error("Failed to apply rule: {}", rule.getName(), e);
                // Continue with other rules
            }
        }

        return result;
    }

    /**
     * Apply a single transformation rule
     */
    private Map<String, Object> applyRule(
            Map<String, Object> data,
            TransformationRule rule) {

        Map<String, Object> result = new HashMap<>(data);

        switch (rule.getRuleType()) {
            case "FIELD_MAPPING":
                return applyFieldMapping(result, rule);
            case "VALUE_MAPPING":
                return applyValueMapping(result, rule);
            case "EXPRESSION":
                return applyExpression(result, rule);
            case "SCRIPT":
                return applyScript(result, rule);
            case "REGEX":
                return applyRegex(result, rule);
            case "CONDITIONAL":
                return applyConditional(result, rule);
            case "AGGREGATION":
                return applyAggregation(result, rule);
            case "ENRICHMENT":
                return applyEnrichment(result, rule);
            default:
                log.warn("Unknown rule type: {}", rule.getRuleType());
                return result;
        }
    }

    /**
     * Apply field mapping rule
     */
    private Map<String, Object> applyFieldMapping(
            Map<String, Object> data,
            TransformationRule rule) {

        String sourceField = rule.getSourceField();
        String targetField = rule.getTargetField();

        Object value = getNestedValue(data, sourceField);
        if (value != null) {
            setNestedValue(data, targetField, value);
            if (!sourceField.equals(targetField)) {
                removeNestedValue(data, sourceField);
            }
        }

        return data;
    }

    /**
     * Apply value mapping rule
     */
    private Map<String, Object> applyValueMapping(
            Map<String, Object> data,
            TransformationRule rule) {

        String field = rule.getSourceField();
        Map<String, Object> mappings = parseValueMappings(rule.getRuleDefinition());

        Object currentValue = getNestedValue(data, field);
        if (currentValue != null && mappings.containsKey(currentValue.toString())) {
            setNestedValue(data, field, mappings.get(currentValue.toString()));
        }

        return data;
    }

    /**
     * Apply expression-based transformation
     */
    private Map<String, Object> applyExpression(
            Map<String, Object> data,
            TransformationRule rule) {

        String expression = rule.getRuleDefinition();
        StandardEvaluationContext context = new StandardEvaluationContext(data);

        try {
            Object result = expressionParser.parseExpression(expression).getValue(context);
            setNestedValue(data, rule.getTargetField(), result);
        } catch (Exception e) {
            log.error("Failed to evaluate expression: {}", expression, e);
        }

        return data;
    }

    /**
     * Apply script-based transformation
     */
    private Map<String, Object> applyScript(
            Map<String, Object> data,
            TransformationRule rule) {

        String scriptType = rule.getScriptType() != null ? rule.getScriptType() : "javascript";
        ScriptEngine engine = scriptEngineManager.getEngineByName(scriptType);

        if (engine != null) {
            try {
                engine.put("data", data);
                engine.eval(rule.getRuleDefinition());
                Object result = engine.get("result");
                if (result != null) {
                    setNestedValue(data, rule.getTargetField(), result);
                }
            } catch (Exception e) {
                log.error("Failed to execute script", e);
            }
        }

        return data;
    }

    /**
     * Apply regex-based transformation
     */
    private Map<String, Object> applyRegex(
            Map<String, Object> data,
            TransformationRule rule) {

        String field = rule.getSourceField();
        String pattern = rule.getRegexPattern();
        String replacement = rule.getRegexReplacement();

        Object value = getNestedValue(data, field);
        if (value != null && pattern != null) {
            Pattern p = Pattern.compile(pattern);
            Matcher m = p.matcher(value.toString());

            if (replacement != null) {
                String result = m.replaceAll(replacement);
                setNestedValue(data, rule.getTargetField(), result);
            } else if (m.find()) {
                // Extract matched groups
                List<String> groups = new ArrayList<>();
                for (int i = 1; i <= m.groupCount(); i++) {
                    groups.add(m.group(i));
                }
                setNestedValue(data, rule.getTargetField(), groups);
            }
        }

        return data;
    }

    /**
     * Apply conditional transformation
     */
    private Map<String, Object> applyConditional(
            Map<String, Object> data,
            TransformationRule rule) {

        String condition = rule.getCondition();
        StandardEvaluationContext context = new StandardEvaluationContext(data);

        try {
            Boolean conditionMet = expressionParser
                .parseExpression(condition)
                .getValue(context, Boolean.class);

            if (Boolean.TRUE.equals(conditionMet)) {
                // Apply transformation if condition is met
                String transformation = rule.getRuleDefinition();
                Object result = expressionParser
                    .parseExpression(transformation)
                    .getValue(context);
                setNestedValue(data, rule.getTargetField(), result);
            }
        } catch (Exception e) {
            log.error("Failed to evaluate conditional rule", e);
        }

        return data;
    }

    /**
     * Apply aggregation transformation
     */
    private Map<String, Object> applyAggregation(
            Map<String, Object> data,
            TransformationRule rule) {

        String aggregationType = rule.getAggregationType();
        String field = rule.getSourceField();

        Object value = getNestedValue(data, field);
        if (value instanceof Collection) {
            Collection<?> collection = (Collection<?>) value;
            Object result = null;

            switch (aggregationType.toUpperCase()) {
                case "SUM":
                    result = calculateSum(collection);
                    break;
                case "AVG":
                    result = calculateAverage(collection);
                    break;
                case "COUNT":
                    result = collection.size();
                    break;
                case "MIN":
                    result = Collections.min((Collection) collection);
                    break;
                case "MAX":
                    result = Collections.max((Collection) collection);
                    break;
                case "CONCAT":
                    result = concatenate(collection);
                    break;
            }

            if (result != null) {
                setNestedValue(data, rule.getTargetField(), result);
            }
        }

        return data;
    }

    /**
     * Apply data enrichment
     */
    private Map<String, Object> applyEnrichment(
            Map<String, Object> data,
            TransformationRule rule) {

        // Add enrichment data based on rule definition
        Map<String, Object> enrichmentData = parseEnrichmentData(rule.getRuleDefinition());

        for (Map.Entry<String, Object> entry : enrichmentData.entrySet()) {
            setNestedValue(data, entry.getKey(), entry.getValue());
        }

        return data;
    }

    /**
     * Convert data to target format
     */
    private Map<String, Object> convertToTargetFormat(
            Map<String, Object> data,
            String targetFormat) {

        switch (targetFormat.toUpperCase()) {
            case "JSON":
                return data; // Already in map format
            case "XML":
                return wrapForXml(data);
            case "CSV":
                return wrapForCsv(data);
            case "AVRO":
                return wrapForAvro(data);
            case "PARQUET":
                return wrapForParquet(data);
            default:
                return data;
        }
    }

    /**
     * Transform XML using XSLT
     */
    public String transformXml(String xmlInput, String xsltTemplate) {
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(new StringReader(xsltTemplate));
            Transformer transformer = factory.newTransformer(xslt);

            Source xml = new StreamSource(new StringReader(xmlInput));
            StringWriter writer = new StringWriter();
            Result result = new StreamResult(writer);

            transformer.transform(xml, result);
            return writer.toString();
        } catch (Exception e) {
            log.error("XML transformation failed", e);
            throw new RuntimeException("XML transformation failed", e);
        }
    }

    /**
     * Helper methods for nested value operations
     */
    private Object getNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        String[] parts = path.split("\\.");
        Object current = data;

        for (String part : parts) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else {
                return null;
            }
        }

        return current;
    }

    @SuppressWarnings("unchecked")
    private void setNestedValue(Map<String, Object> data, String path, Object value) {
        if (path == null || path.isEmpty()) {
            return;
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            if (!current.containsKey(part)) {
                current.put(part, new HashMap<String, Object>());
            }
            current = (Map<String, Object>) current.get(part);
        }

        current.put(parts[parts.length - 1], value);
    }

    @SuppressWarnings("unchecked")
    private void removeNestedValue(Map<String, Object> data, String path) {
        if (path == null || path.isEmpty()) {
            return;
        }

        String[] parts = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            if (next instanceof Map) {
                current = (Map<String, Object>) next;
            } else {
                return;
            }
        }

        current.remove(parts[parts.length - 1]);
    }

    /**
     * Parse value mappings from rule definition
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseValueMappings(String definition) {
        try {
            return objectMapper.readValue(definition, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse value mappings", e);
            return new HashMap<>();
        }
    }

    /**
     * Parse enrichment data from rule definition
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseEnrichmentData(String definition) {
        try {
            return objectMapper.readValue(definition, Map.class);
        } catch (Exception e) {
            log.error("Failed to parse enrichment data", e);
            return new HashMap<>();
        }
    }

    /**
     * Calculate sum of numeric collection
     */
    private Double calculateSum(Collection<?> collection) {
        return collection.stream()
            .filter(v -> v instanceof Number)
            .mapToDouble(v -> ((Number) v).doubleValue())
            .sum();
    }

    /**
     * Calculate average of numeric collection
     */
    private Double calculateAverage(Collection<?> collection) {
        return collection.stream()
            .filter(v -> v instanceof Number)
            .mapToDouble(v -> ((Number) v).doubleValue())
            .average()
            .orElse(0.0);
    }

    /**
     * Concatenate collection elements
     */
    private String concatenate(Collection<?> collection) {
        return collection.stream()
            .map(Object::toString)
            .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);
    }

    /**
     * Wrap data for XML format
     */
    private Map<String, Object> wrapForXml(Map<String, Object> data) {
        Map<String, Object> wrapped = new HashMap<>();
        wrapped.put("root", data);
        return wrapped;
    }

    /**
     * Wrap data for CSV format
     */
    private Map<String, Object> wrapForCsv(Map<String, Object> data) {
        // Flatten nested structure for CSV
        Map<String, Object> flattened = new HashMap<>();
        flattenMap("", data, flattened);
        return flattened;
    }

    /**
     * Wrap data for Avro format
     */
    private Map<String, Object> wrapForAvro(Map<String, Object> data) {
        // Add Avro-specific metadata
        data.put("_avro_schema", generateAvroSchema(data));
        return data;
    }

    /**
     * Wrap data for Parquet format
     */
    private Map<String, Object> wrapForParquet(Map<String, Object> data) {
        // Add Parquet-specific metadata
        data.put("_parquet_schema", generateParquetSchema(data));
        return data;
    }

    /**
     * Flatten nested map structure
     */
    private void flattenMap(String prefix, Map<String, Object> nested, Map<String, Object> flat) {
        for (Map.Entry<String, Object> entry : nested.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                flattenMap(key, (Map<String, Object>) value, flat);
            } else {
                flat.put(key, value);
            }
        }
    }

    private String generateAvroSchema(Map<String, Object> data) {
        // Generate Avro schema based on data structure
        return "{}"; // Simplified
    }

    private String generateParquetSchema(Map<String, Object> data) {
        // Generate Parquet schema based on data structure
        return "{}"; // Simplified
    }
}