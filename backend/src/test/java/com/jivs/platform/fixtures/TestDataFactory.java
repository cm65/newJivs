package com.jivs.platform.fixtures;

import com.jivs.platform.domain.*;
import com.jivs.platform.dto.*;
import com.github.javafaker.Faker;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Comprehensive test data factory for all JiVS domain models.
 * Provides consistent, realistic test data for all testing layers.
 *
 * Features:
 * - Builder pattern for flexible object construction
 * - Faker for realistic random data
 * - Preset scenarios for common test cases
 * - Support for bulk data generation
 *
 * @since Day 10 of Continuous Testing Implementation
 */
public class TestDataFactory {

    private static final Faker faker = new Faker();
    private static final Random random = new Random();

    /**
     * User test data builder
     */
    public static class UserBuilder {
        private Long id = faker.number().randomNumber();
        private String username = faker.name().username();
        private String email = faker.internet().emailAddress();
        private String firstName = faker.name().firstName();
        private String lastName = faker.name().lastName();
        private String password = "Password123!";
        private Set<String> roles = new HashSet<>(Arrays.asList("ROLE_USER"));
        private boolean enabled = true;
        private LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(365));

        public UserBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder withUsername(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder withEmail(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder withRoles(String... roles) {
            this.roles = new HashSet<>(Arrays.asList(roles));
            return this;
        }

        public UserBuilder asAdmin() {
            this.roles = new HashSet<>(Arrays.asList("ROLE_ADMIN"));
            return this;
        }

        public UserBuilder asDataEngineer() {
            this.roles = new HashSet<>(Arrays.asList("ROLE_DATA_ENGINEER"));
            return this;
        }

        public UserBuilder asComplianceOfficer() {
            this.roles = new HashSet<>(Arrays.asList("ROLE_COMPLIANCE_OFFICER"));
            return this;
        }

        public User build() {
            User user = new User();
            user.setId(id);
            user.setUsername(username);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setPassword(password);
            user.setEnabled(enabled);
            user.setCreatedAt(createdAt);
            // Note: In real implementation, map roles properly
            return user;
        }
    }

    /**
     * Extraction test data builder
     */
    public static class ExtractionBuilder {
        private Long id = faker.number().randomNumber();
        private String name = "Extraction_" + faker.company().name().replace(" ", "_");
        private ExtractionStatus status = ExtractionStatus.PENDING;
        private String sourceType = randomFrom("JDBC", "SAP", "FILE", "API");
        private Map<String, Object> connectionConfig = defaultConnectionConfig();
        private String extractionQuery = "SELECT * FROM " + faker.lorem().word();
        private Long recordsExtracted = 0L;
        private LocalDateTime startTime = null;
        private LocalDateTime endTime = null;
        private LocalDateTime createdAt = LocalDateTime.now();
        private String createdBy = faker.name().username();

        private Map<String, Object> defaultConnectionConfig() {
            Map<String, Object> config = new HashMap<>();
            if ("JDBC".equals(sourceType)) {
                config.put("url", "jdbc:postgresql://localhost:5432/" + faker.lorem().word());
                config.put("username", faker.name().username());
                config.put("password", "password");
                config.put("driver", "org.postgresql.Driver");
            } else if ("SAP".equals(sourceType)) {
                config.put("host", faker.internet().ipV4Address());
                config.put("systemNumber", "00");
                config.put("client", "100");
                config.put("username", faker.name().username());
            } else if ("FILE".equals(sourceType)) {
                config.put("path", "/data/imports/" + faker.file().fileName());
                config.put("format", randomFrom("CSV", "JSON", "XML", "PARQUET"));
                config.put("delimiter", ",");
            } else if ("API".equals(sourceType)) {
                config.put("endpoint", "https://api.example.com/" + faker.lorem().word());
                config.put("method", "GET");
                config.put("authType", "Bearer");
            }
            return config;
        }

        public ExtractionBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public ExtractionBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public ExtractionBuilder withStatus(ExtractionStatus status) {
            this.status = status;
            return this;
        }

        public ExtractionBuilder asRunning() {
            this.status = ExtractionStatus.RUNNING;
            this.startTime = LocalDateTime.now().minusMinutes(random.nextInt(60));
            this.recordsExtracted = (long) faker.number().numberBetween(1000, 100000);
            return this;
        }

        public ExtractionBuilder asCompleted() {
            this.status = ExtractionStatus.COMPLETED;
            this.startTime = LocalDateTime.now().minusHours(2);
            this.endTime = LocalDateTime.now().minusHours(1);
            this.recordsExtracted = (long) faker.number().numberBetween(10000, 1000000);
            return this;
        }

        public ExtractionBuilder asFailed() {
            this.status = ExtractionStatus.FAILED;
            this.startTime = LocalDateTime.now().minusHours(1);
            this.endTime = LocalDateTime.now().minusMinutes(30);
            return this;
        }

        public ExtractionBuilder withSourceType(String sourceType) {
            this.sourceType = sourceType;
            this.connectionConfig = defaultConnectionConfig();
            return this;
        }

        public Extraction build() {
            Extraction extraction = new Extraction();
            extraction.setId(id);
            extraction.setName(name);
            extraction.setStatus(status);
            extraction.setSourceType(sourceType);
            extraction.setConnectionConfig(connectionConfig);
            extraction.setExtractionQuery(extractionQuery);
            extraction.setRecordsExtracted(recordsExtracted);
            extraction.setStartTime(startTime);
            extraction.setEndTime(endTime);
            extraction.setCreatedAt(createdAt);
            extraction.setCreatedBy(createdBy);
            return extraction;
        }
    }

    /**
     * Migration test data builder
     */
    public static class MigrationBuilder {
        private Long id = faker.number().randomNumber();
        private String name = "Migration_" + faker.company().name().replace(" ", "_");
        private MigrationStatus status = MigrationStatus.PENDING;
        private String phase = "PLANNING";
        private int progress = 0;
        private String sourceSystem = randomFrom(
            "Oracle Database 12c", "SQL Server 2019", "MySQL 8.0", "PostgreSQL 13"
        );
        private String targetSystem = randomFrom(
            "PostgreSQL 15", "MySQL 8.0", "MongoDB 5.0", "Snowflake"
        );
        private Map<String, Object> sourceConfig = defaultSourceConfig();
        private Map<String, Object> targetConfig = defaultTargetConfig();
        private Long recordsMigrated = 0L;
        private Long totalRecords = (long) faker.number().numberBetween(10000, 1000000);
        private LocalDateTime startTime = null;
        private LocalDateTime endTime = null;
        private LocalDateTime createdAt = LocalDateTime.now();
        private String createdBy = faker.name().username();

        private Map<String, Object> defaultSourceConfig() {
            Map<String, Object> config = new HashMap<>();
            config.put("connectionUrl", "jdbc:oracle:thin:@localhost:1521:ORCL");
            config.put("username", "source_user");
            config.put("schema", "PROD_SCHEMA");
            config.put("tables", Arrays.asList("customers", "orders", "products"));
            return config;
        }

        private Map<String, Object> defaultTargetConfig() {
            Map<String, Object> config = new HashMap<>();
            config.put("connectionUrl", "jdbc:postgresql://localhost:5432/target_db");
            config.put("username", "target_user");
            config.put("schema", "public");
            return config;
        }

        public MigrationBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public MigrationBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public MigrationBuilder withStatus(MigrationStatus status) {
            this.status = status;
            return this;
        }

        public MigrationBuilder inPhase(String phase, int progress) {
            this.phase = phase;
            this.progress = Math.min(100, Math.max(0, progress));
            return this;
        }

        public MigrationBuilder asRunning() {
            this.status = MigrationStatus.RUNNING;
            this.phase = randomFrom("EXTRACTION", "TRANSFORMATION", "LOADING");
            this.progress = faker.number().numberBetween(10, 90);
            this.startTime = LocalDateTime.now().minusMinutes(random.nextInt(120));
            this.recordsMigrated = (long) (totalRecords * progress / 100);
            return this;
        }

        public MigrationBuilder asCompleted() {
            this.status = MigrationStatus.COMPLETED;
            this.phase = "CLEANUP";
            this.progress = 100;
            this.startTime = LocalDateTime.now().minusHours(3);
            this.endTime = LocalDateTime.now().minusHours(1);
            this.recordsMigrated = totalRecords;
            return this;
        }

        public MigrationBuilder asFailed() {
            this.status = MigrationStatus.FAILED;
            this.phase = randomFrom("VALIDATION", "EXTRACTION", "TRANSFORMATION");
            this.progress = faker.number().numberBetween(10, 60);
            this.startTime = LocalDateTime.now().minusHours(2);
            this.endTime = LocalDateTime.now().minusHours(1);
            this.recordsMigrated = (long) (totalRecords * progress / 100);
            return this;
        }

        public Migration build() {
            Migration migration = new Migration();
            migration.setId(id);
            migration.setName(name);
            migration.setStatus(status);
            migration.setPhase(phase);
            migration.setProgress(progress);
            migration.setSourceSystem(sourceSystem);
            migration.setTargetSystem(targetSystem);
            migration.setSourceConfig(sourceConfig);
            migration.setTargetConfig(targetConfig);
            migration.setRecordsMigrated(recordsMigrated);
            migration.setTotalRecords(totalRecords);
            migration.setStartTime(startTime);
            migration.setEndTime(endTime);
            migration.setCreatedAt(createdAt);
            migration.setCreatedBy(createdBy);
            return migration;
        }
    }

    /**
     * Data Quality Rule test data builder
     */
    public static class DataQualityRuleBuilder {
        private Long id = faker.number().randomNumber();
        private String name = "Rule_" + faker.lorem().word();
        private String dimension = randomFrom(
            "COMPLETENESS", "ACCURACY", "CONSISTENCY", "VALIDITY", "UNIQUENESS", "TIMELINESS"
        );
        private String ruleType = randomFrom(
            "NULL_CHECK", "FORMAT_VALIDATION", "RANGE_CHECK", "UNIQUENESS_CHECK",
            "REFERENTIAL_INTEGRITY", "BUSINESS_RULE"
        );
        private Map<String, Object> configuration = defaultConfiguration();
        private boolean enabled = true;
        private int severity = randomFrom(1, 2, 3, 4, 5);
        private LocalDateTime createdAt = LocalDateTime.now().minusDays(random.nextInt(30));

        private Map<String, Object> defaultConfiguration() {
            Map<String, Object> config = new HashMap<>();
            config.put("table", "customers");
            config.put("column", faker.lorem().word());

            switch (ruleType) {
                case "NULL_CHECK":
                    config.put("allowNull", false);
                    break;
                case "FORMAT_VALIDATION":
                    config.put("pattern", "^[A-Z]{2}[0-9]{6}$");
                    break;
                case "RANGE_CHECK":
                    config.put("min", 0);
                    config.put("max", 100);
                    break;
                case "UNIQUENESS_CHECK":
                    config.put("scope", "table");
                    break;
                case "REFERENTIAL_INTEGRITY":
                    config.put("referenceTable", "orders");
                    config.put("referenceColumn", "customer_id");
                    break;
                case "BUSINESS_RULE":
                    config.put("expression", "column1 + column2 <= 100");
                    break;
            }
            return config;
        }

        public DataQualityRuleBuilder withName(String name) {
            this.name = name;
            return this;
        }

        public DataQualityRuleBuilder withDimension(String dimension) {
            this.dimension = dimension;
            return this;
        }

        public DataQualityRuleBuilder withRuleType(String ruleType) {
            this.ruleType = ruleType;
            this.configuration = defaultConfiguration();
            return this;
        }

        public DataQualityRuleBuilder withSeverity(int severity) {
            this.severity = severity;
            return this;
        }

        public DataQualityRule build() {
            DataQualityRule rule = new DataQualityRule();
            rule.setId(id);
            rule.setName(name);
            rule.setDimension(dimension);
            rule.setRuleType(ruleType);
            rule.setConfiguration(configuration);
            rule.setEnabled(enabled);
            rule.setSeverity(severity);
            rule.setCreatedAt(createdAt);
            return rule;
        }
    }

    /**
     * Compliance Request test data builder (GDPR/CCPA)
     */
    public static class ComplianceRequestBuilder {
        private Long id = faker.number().randomNumber();
        private String requestType = randomFrom(
            "ACCESS", "ERASURE", "RECTIFICATION", "PORTABILITY", "OBJECTION"
        );
        private String regulation = randomFrom("GDPR", "CCPA");
        private String subjectEmail = faker.internet().emailAddress();
        private String subjectName = faker.name().fullName();
        private String status = "PENDING";
        private Map<String, Object> requestData = defaultRequestData();
        private Map<String, Object> responseData = null;
        private LocalDateTime requestDate = LocalDateTime.now().minusDays(random.nextInt(30));
        private LocalDateTime dueDate = requestDate.plusDays(30);
        private LocalDateTime completedDate = null;
        private String assignedTo = faker.name().username();

        private Map<String, Object> defaultRequestData() {
            Map<String, Object> data = new HashMap<>();
            data.put("reason", faker.lorem().sentence());
            data.put("identityVerified", true);
            data.put("dataSources", Arrays.asList("CRM", "ERP", "Analytics"));

            if ("ERASURE".equals(requestType)) {
                data.put("confirmationToken", UUID.randomUUID().toString());
                data.put("retentionException", false);
            } else if ("PORTABILITY".equals(requestType)) {
                data.put("format", randomFrom("JSON", "CSV", "XML"));
                data.put("deliveryMethod", randomFrom("EMAIL", "DOWNLOAD", "API"));
            }
            return data;
        }

        public ComplianceRequestBuilder withRequestType(String requestType) {
            this.requestType = requestType;
            this.requestData = defaultRequestData();
            return this;
        }

        public ComplianceRequestBuilder withRegulation(String regulation) {
            this.regulation = regulation;
            return this;
        }

        public ComplianceRequestBuilder asProcessing() {
            this.status = "PROCESSING";
            return this;
        }

        public ComplianceRequestBuilder asCompleted() {
            this.status = "COMPLETED";
            this.completedDate = LocalDateTime.now().minusDays(random.nextInt(5));
            this.responseData = new HashMap<>();
            responseData.put("recordsFound", faker.number().numberBetween(100, 10000));
            responseData.put("systemsSearched", 12);
            responseData.put("processingTime", "2 hours 15 minutes");
            return this;
        }

        public ComplianceRequestBuilder asRejected() {
            this.status = "REJECTED";
            this.completedDate = LocalDateTime.now().minusDays(1);
            this.responseData = new HashMap<>();
            responseData.put("rejectionReason", "Identity verification failed");
            return this;
        }

        public ComplianceRequest build() {
            ComplianceRequest request = new ComplianceRequest();
            request.setId(id);
            request.setRequestType(requestType);
            request.setRegulation(regulation);
            request.setSubjectEmail(subjectEmail);
            request.setSubjectName(subjectName);
            request.setStatus(status);
            request.setRequestData(requestData);
            request.setResponseData(responseData);
            request.setRequestDate(requestDate);
            request.setDueDate(dueDate);
            request.setCompletedDate(completedDate);
            request.setAssignedTo(assignedTo);
            return request;
        }
    }

    // Static factory methods for quick object creation

    public static User createUser() {
        return new UserBuilder().build();
    }

    public static User createAdmin() {
        return new UserBuilder().asAdmin().build();
    }

    public static Extraction createExtraction() {
        return new ExtractionBuilder().build();
    }

    public static Extraction createRunningExtraction() {
        return new ExtractionBuilder().asRunning().build();
    }

    public static Migration createMigration() {
        return new MigrationBuilder().build();
    }

    public static Migration createRunningMigration() {
        return new MigrationBuilder().asRunning().build();
    }

    public static DataQualityRule createDataQualityRule() {
        return new DataQualityRuleBuilder().build();
    }

    public static ComplianceRequest createComplianceRequest() {
        return new ComplianceRequestBuilder().build();
    }

    // Bulk data generation methods

    public static List<User> createUsers(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> createUser())
            .collect(Collectors.toList());
    }

    public static List<Extraction> createExtractions(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                int rand = random.nextInt(4);
                if (rand == 0) return new ExtractionBuilder().asRunning().build();
                if (rand == 1) return new ExtractionBuilder().asCompleted().build();
                if (rand == 2) return new ExtractionBuilder().asFailed().build();
                return createExtraction();
            })
            .collect(Collectors.toList());
    }

    public static List<Migration> createMigrations(int count) {
        return IntStream.range(0, count)
            .mapToObj(i -> {
                int rand = random.nextInt(4);
                if (rand == 0) return new MigrationBuilder().asRunning().build();
                if (rand == 1) return new MigrationBuilder().asCompleted().build();
                if (rand == 2) return new MigrationBuilder().asFailed().build();
                return createMigration();
            })
            .collect(Collectors.toList());
    }

    // Scenario-based test data sets

    /**
     * Creates a complete test scenario for end-to-end testing
     */
    public static class TestScenario {
        public User admin;
        public User dataEngineer;
        public User complianceOfficer;
        public List<Extraction> extractions;
        public List<Migration> migrations;
        public List<DataQualityRule> qualityRules;
        public List<ComplianceRequest> complianceRequests;

        public static TestScenario createCompleteScenario() {
            TestScenario scenario = new TestScenario();

            // Create users with specific roles
            scenario.admin = new UserBuilder()
                .withUsername("admin")
                .withEmail("admin@jivs.com")
                .asAdmin()
                .build();

            scenario.dataEngineer = new UserBuilder()
                .withUsername("engineer1")
                .withEmail("engineer@jivs.com")
                .asDataEngineer()
                .build();

            scenario.complianceOfficer = new UserBuilder()
                .withUsername("compliance1")
                .withEmail("compliance@jivs.com")
                .asComplianceOfficer()
                .build();

            // Create mixed status extractions
            scenario.extractions = Arrays.asList(
                new ExtractionBuilder().withName("Customer_Data_Extract").asCompleted().build(),
                new ExtractionBuilder().withName("Order_History_Extract").asRunning().build(),
                new ExtractionBuilder().withName("Product_Catalog_Extract").build(),
                new ExtractionBuilder().withName("Failed_Extract").asFailed().build()
            );

            // Create migrations in various phases
            scenario.migrations = Arrays.asList(
                new MigrationBuilder()
                    .withName("Oracle_to_Postgres")
                    .asRunning()
                    .inPhase("TRANSFORMATION", 65)
                    .build(),
                new MigrationBuilder()
                    .withName("MySQL_to_MongoDB")
                    .asCompleted()
                    .build(),
                new MigrationBuilder()
                    .withName("SQLServer_to_Snowflake")
                    .inPhase("PLANNING", 10)
                    .build()
            );

            // Create quality rules for all dimensions
            scenario.qualityRules = Arrays.asList(
                new DataQualityRuleBuilder()
                    .withName("Email_Format_Check")
                    .withDimension("VALIDITY")
                    .withRuleType("FORMAT_VALIDATION")
                    .build(),
                new DataQualityRuleBuilder()
                    .withName("Customer_ID_Uniqueness")
                    .withDimension("UNIQUENESS")
                    .withRuleType("UNIQUENESS_CHECK")
                    .build(),
                new DataQualityRuleBuilder()
                    .withName("Order_Amount_Range")
                    .withDimension("ACCURACY")
                    .withRuleType("RANGE_CHECK")
                    .build()
            );

            // Create GDPR/CCPA requests
            scenario.complianceRequests = Arrays.asList(
                new ComplianceRequestBuilder()
                    .withRequestType("ACCESS")
                    .withRegulation("GDPR")
                    .asCompleted()
                    .build(),
                new ComplianceRequestBuilder()
                    .withRequestType("ERASURE")
                    .withRegulation("GDPR")
                    .asProcessing()
                    .build(),
                new ComplianceRequestBuilder()
                    .withRequestType("PORTABILITY")
                    .withRegulation("CCPA")
                    .build()
            );

            return scenario;
        }
    }

    // Performance testing data generators

    /**
     * Generates large datasets for performance testing
     */
    public static class PerformanceTestData {

        public static List<Map<String, Object>> generateBulkRecords(int count) {
            return IntStream.range(0, count)
                .parallel()
                .mapToObj(i -> {
                    Map<String, Object> record = new HashMap<>();
                    record.put("id", UUID.randomUUID().toString());
                    record.put("customerId", faker.number().numberBetween(1000, 99999));
                    record.put("firstName", faker.name().firstName());
                    record.put("lastName", faker.name().lastName());
                    record.put("email", faker.internet().emailAddress());
                    record.put("phone", faker.phoneNumber().phoneNumber());
                    record.put("address", faker.address().fullAddress());
                    record.put("city", faker.address().city());
                    record.put("country", faker.address().country());
                    record.put("orderCount", faker.number().numberBetween(0, 100));
                    record.put("totalSpent", faker.number().randomDouble(2, 0, 100000));
                    record.put("lastOrderDate", LocalDateTime.now().minusDays(random.nextInt(365)));
                    record.put("accountStatus", randomFrom("ACTIVE", "INACTIVE", "SUSPENDED"));
                    record.put("creditScore", faker.number().numberBetween(300, 850));
                    record.put("preferredChannel", randomFrom("EMAIL", "SMS", "PHONE", "MAIL"));
                    return record;
                })
                .collect(Collectors.toList());
        }

        public static String generateLargeCSV(int rows) {
            StringBuilder csv = new StringBuilder();
            csv.append("id,customer_id,first_name,last_name,email,phone,order_count,total_spent\n");

            for (int i = 0; i < rows; i++) {
                csv.append(UUID.randomUUID()).append(",");
                csv.append(faker.number().numberBetween(1000, 99999)).append(",");
                csv.append(faker.name().firstName()).append(",");
                csv.append(faker.name().lastName()).append(",");
                csv.append(faker.internet().emailAddress()).append(",");
                csv.append(faker.phoneNumber().phoneNumber()).append(",");
                csv.append(faker.number().numberBetween(0, 100)).append(",");
                csv.append(faker.number().randomDouble(2, 0, 100000)).append("\n");
            }

            return csv.toString();
        }
    }

    // Helper methods

    private static <T> T randomFrom(T... values) {
        return values[random.nextInt(values.length)];
    }

    private static int randomFrom(int... values) {
        return values[random.nextInt(values.length)];
    }
}