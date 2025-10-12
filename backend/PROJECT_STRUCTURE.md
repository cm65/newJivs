# Backend Project Structure

```
backend/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/jivs/platform/
│   │   │   ├── JivsPlatformApplication.java
│   │   │   │
│   │   │   ├── config/                    # Configuration classes
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── DatabaseConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── ElasticsearchConfig.java
│   │   │   │   ├── RabbitMQConfig.java
│   │   │   │   ├── QuartzConfig.java
│   │   │   │   ├── CamundaConfig.java
│   │   │   │   ├── SwaggerConfig.java
│   │   │   │   ├── WebConfig.java
│   │   │   │   └── AsyncConfig.java
│   │   │   │
│   │   │   ├── common/                    # Common utilities
│   │   │   │   ├── exception/
│   │   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   │   ├── ResourceNotFoundException.java
│   │   │   │   │   ├── ValidationException.java
│   │   │   │   │   ├── UnauthorizedException.java
│   │   │   │   │   └── BusinessException.java
│   │   │   │   ├── util/
│   │   │   │   │   ├── DateTimeUtil.java
│   │   │   │   │   ├── StringUtil.java
│   │   │   │   │   ├── FileUtil.java
│   │   │   │   │   ├── JsonUtil.java
│   │   │   │   │   └── CryptoUtil.java
│   │   │   │   ├── constant/
│   │   │   │   │   ├── Constants.java
│   │   │   │   │   ├── ErrorCodes.java
│   │   │   │   │   └── MessageConstants.java
│   │   │   │   └── dto/
│   │   │   │       ├── PageResponse.java
│   │   │   │       ├── ApiResponse.java
│   │   │   │       └── ErrorResponse.java
│   │   │   │
│   │   │   ├── security/                  # Security components
│   │   │   │   ├── JwtAuthenticationFilter.java
│   │   │   │   ├── JwtTokenProvider.java
│   │   │   │   ├── UserDetailsServiceImpl.java
│   │   │   │   ├── SecurityContextHolder.java
│   │   │   │   └── CustomAccessDeniedHandler.java
│   │   │   │
│   │   │   ├── domain/                    # Domain entities
│   │   │   │   ├── user/
│   │   │   │   │   ├── User.java
│   │   │   │   │   ├── Role.java
│   │   │   │   │   └── Permission.java
│   │   │   │   ├── businessobject/
│   │   │   │   │   ├── BusinessObject.java
│   │   │   │   │   ├── BusinessObjectDefinition.java
│   │   │   │   │   ├── BusinessObjectField.java
│   │   │   │   │   ├── BusinessObjectRelationship.java
│   │   │   │   │   └── BusinessObjectVersion.java
│   │   │   │   ├── extraction/
│   │   │   │   │   ├── ExtractionJob.java
│   │   │   │   │   ├── ExtractionConfig.java
│   │   │   │   │   ├── ExtractionLog.java
│   │   │   │   │   └── DataSource.java
│   │   │   │   ├── migration/
│   │   │   │   │   ├── MigrationProject.java
│   │   │   │   │   ├── MigrationJob.java
│   │   │   │   │   ├── MigrationPhase.java
│   │   │   │   │   ├── MigrationTask.java
│   │   │   │   │   └── MigrationLog.java
│   │   │   │   ├── dataquality/
│   │   │   │   │   ├── DataQualityRule.java
│   │   │   │   │   ├── DataQualityCheck.java
│   │   │   │   │   ├── DataQualityResult.java
│   │   │   │   │   └── DuplicateRecord.java
│   │   │   │   ├── retention/
│   │   │   │   │   ├── RetentionPolicy.java
│   │   │   │   │   ├── RetentionRule.java
│   │   │   │   │   ├── RetentionSchedule.java
│   │   │   │   │   └── DataLifecycleRecord.java
│   │   │   │   ├── document/
│   │   │   │   │   ├── Document.java
│   │   │   │   │   ├── DocumentMetadata.java
│   │   │   │   │   ├── DocumentVersion.java
│   │   │   │   │   └── DocumentCategory.java
│   │   │   │   ├── compliance/
│   │   │   │   │   ├── CompliancePolicy.java
│   │   │   │   │   ├── DataSubjectRequest.java
│   │   │   │   │   ├── ConsentRecord.java
│   │   │   │   │   └── PrivacyImpactAssessment.java
│   │   │   │   └── audit/
│   │   │   │       ├── AuditLog.java
│   │   │   │       └── AuditEvent.java
│   │   │   │
│   │   │   ├── repository/                # Data access layer
│   │   │   │   ├── UserRepository.java
│   │   │   │   ├── RoleRepository.java
│   │   │   │   ├── BusinessObjectRepository.java
│   │   │   │   ├── ExtractionJobRepository.java
│   │   │   │   ├── MigrationProjectRepository.java
│   │   │   │   ├── DataQualityRuleRepository.java
│   │   │   │   ├── RetentionPolicyRepository.java
│   │   │   │   ├── DocumentRepository.java
│   │   │   │   ├── CompliancePolicyRepository.java
│   │   │   │   └── AuditLogRepository.java
│   │   │   │
│   │   │   ├── service/                   # Business logic layer
│   │   │   │   ├── extraction/
│   │   │   │   │   ├── ExtractionService.java
│   │   │   │   │   ├── ExtractionOrchestrator.java
│   │   │   │   │   ├── ConnectorFactory.java
│   │   │   │   │   ├── SapConnector.java
│   │   │   │   │   ├── JdbcConnector.java
│   │   │   │   │   ├── FileConnector.java
│   │   │   │   │   └── ApiConnector.java
│   │   │   │   ├── transformation/
│   │   │   │   │   ├── TransformationService.java
│   │   │   │   │   ├── TransformationEngine.java
│   │   │   │   │   ├── RuleEngine.java
│   │   │   │   │   ├── ValidationEngine.java
│   │   │   │   │   ├── EnrichmentEngine.java
│   │   │   │   │   └── DataCleansingService.java
│   │   │   │   ├── migration/
│   │   │   │   │   ├── MigrationService.java
│   │   │   │   │   ├── MigrationOrchestrator.java
│   │   │   │   │   ├── WorkflowEngine.java
│   │   │   │   │   ├── StateManager.java
│   │   │   │   │   └── RollbackManager.java
│   │   │   │   ├── businessobject/
│   │   │   │   │   ├── BusinessObjectService.java
│   │   │   │   │   ├── ObjectDefinitionService.java
│   │   │   │   │   ├── ObjectMappingService.java
│   │   │   │   │   └── RelationshipManager.java
│   │   │   │   ├── dataquality/
│   │   │   │   │   ├── DataQualityService.java
│   │   │   │   │   ├── ProfilingEngine.java
│   │   │   │   │   ├── DeduplicationEngine.java
│   │   │   │   │   ├── StandardizationEngine.java
│   │   │   │   │   └── MatchingEngine.java
│   │   │   │   ├── retention/
│   │   │   │   │   ├── RetentionService.java
│   │   │   │   │   ├── LifecycleManager.java
│   │   │   │   │   ├── PolicyEngine.java
│   │   │   │   │   ├── DeletionService.java
│   │   │   │   │   └── LegalHoldManager.java
│   │   │   │   ├── document/
│   │   │   │   │   ├── DocumentService.java
│   │   │   │   │   ├── DocumentIngestionService.java
│   │   │   │   │   ├── MetadataExtractor.java
│   │   │   │   │   ├── FullTextIndexer.java
│   │   │   │   │   └── VersionControlService.java
│   │   │   │   ├── storage/
│   │   │   │   │   ├── StorageService.java
│   │   │   │   │   ├── LocalStorageService.java
│   │   │   │   │   ├── S3StorageService.java
│   │   │   │   │   ├── AzureStorageService.java
│   │   │   │   │   └── GcpStorageService.java
│   │   │   │   ├── encryption/
│   │   │   │   │   ├── EncryptionService.java
│   │   │   │   │   ├── KeyManagementService.java
│   │   │   │   │   └── FieldEncryptionService.java
│   │   │   │   ├── compliance/
│   │   │   │   │   ├── ComplianceService.java
│   │   │   │   │   ├── GdprService.java
│   │   │   │   │   ├── CcpaService.java
│   │   │   │   │   ├── DataDiscoveryService.java
│   │   │   │   │   └── ConsentManager.java
│   │   │   │   ├── search/
│   │   │   │   │   ├── SearchService.java
│   │   │   │   │   ├── IndexManager.java
│   │   │   │   │   ├── QueryBuilder.java
│   │   │   │   │   └── ResultFormatter.java
│   │   │   │   ├── analytics/
│   │   │   │   │   ├── AnalyticsService.java
│   │   │   │   │   ├── MetricsCollector.java
│   │   │   │   │   ├── ReportGenerator.java
│   │   │   │   │   └── DashboardService.java
│   │   │   │   ├── audit/
│   │   │   │   │   ├── AuditService.java
│   │   │   │   │   ├── AuditLogger.java
│   │   │   │   │   └── AuditQueryService.java
│   │   │   │   ├── user/
│   │   │   │   │   ├── UserService.java
│   │   │   │   │   ├── RoleService.java
│   │   │   │   │   └── PermissionService.java
│   │   │   │   └── notification/
│   │   │   │       ├── NotificationService.java
│   │   │   │       ├── EmailService.java
│   │   │   │       └── WebSocketService.java
│   │   │   │
│   │   │   ├── controller/                # REST API Controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── ExtractionController.java
│   │   │   │   ├── MigrationController.java
│   │   │   │   ├── BusinessObjectController.java
│   │   │   │   ├── DataQualityController.java
│   │   │   │   ├── RetentionController.java
│   │   │   │   ├── DocumentController.java
│   │   │   │   ├── ComplianceController.java
│   │   │   │   ├── SearchController.java
│   │   │   │   ├── AnalyticsController.java
│   │   │   │   └── AuditController.java
│   │   │   │
│   │   │   ├── dto/                       # Data Transfer Objects
│   │   │   │   ├── auth/
│   │   │   │   │   ├── LoginRequest.java
│   │   │   │   │   ├── LoginResponse.java
│   │   │   │   │   ├── RegisterRequest.java
│   │   │   │   │   └── TokenRefreshRequest.java
│   │   │   │   ├── extraction/
│   │   │   │   │   ├── ExtractionJobDTO.java
│   │   │   │   │   ├── ExtractionConfigDTO.java
│   │   │   │   │   ├── DataSourceDTO.java
│   │   │   │   │   └── ExtractionStatusDTO.java
│   │   │   │   ├── migration/
│   │   │   │   │   ├── MigrationProjectDTO.java
│   │   │   │   │   ├── MigrationJobDTO.java
│   │   │   │   │   └── MigrationStatusDTO.java
│   │   │   │   ├── businessobject/
│   │   │   │   │   ├── BusinessObjectDTO.java
│   │   │   │   │   └── ObjectDefinitionDTO.java
│   │   │   │   ├── dataquality/
│   │   │   │   │   ├── DataQualityRuleDTO.java
│   │   │   │   │   └── DataQualityResultDTO.java
│   │   │   │   ├── retention/
│   │   │   │   │   ├── RetentionPolicyDTO.java
│   │   │   │   │   └── RetentionRuleDTO.java
│   │   │   │   ├── document/
│   │   │   │   │   ├── DocumentDTO.java
│   │   │   │   │   └── DocumentMetadataDTO.java
│   │   │   │   ├── compliance/
│   │   │   │   │   ├── DataSubjectRequestDTO.java
│   │   │   │   │   └── ConsentRecordDTO.java
│   │   │   │   └── user/
│   │   │   │       ├── UserDTO.java
│   │   │   │       └── RoleDTO.java
│   │   │   │
│   │   │   ├── mapper/                    # DTO Mappers (MapStruct)
│   │   │   │   ├── UserMapper.java
│   │   │   │   ├── ExtractionMapper.java
│   │   │   │   ├── MigrationMapper.java
│   │   │   │   ├── BusinessObjectMapper.java
│   │   │   │   └── DocumentMapper.java
│   │   │   │
│   │   │   ├── job/                       # Scheduled Jobs
│   │   │   │   ├── RetentionScanJob.java
│   │   │   │   ├── DataQualityJob.java
│   │   │   │   ├── ComplianceCheckJob.java
│   │   │   │   └── CleanupJob.java
│   │   │   │
│   │   │   ├── event/                     # Domain Events
│   │   │   │   ├── ExtractionCompletedEvent.java
│   │   │   │   ├── MigrationStartedEvent.java
│   │   │   │   ├── DataQualityIssueEvent.java
│   │   │   │   └── RetentionExpiredEvent.java
│   │   │   │
│   │   │   ├── listener/                  # Event Listeners
│   │   │   │   ├── ExtractionEventListener.java
│   │   │   │   ├── MigrationEventListener.java
│   │   │   │   └── AuditEventListener.java
│   │   │   │
│   │   │   └── aspect/                    # AOP Aspects
│   │   │       ├── LoggingAspect.java
│   │   │       ├── AuditAspect.java
│   │   │       └── PerformanceAspect.java
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       ├── application-test.yml
│   │       ├── db/
│   │       │   └── migration/             # Flyway migrations
│   │       │       ├── V1__Initial_schema.sql
│   │       │       ├── V2__Business_objects.sql
│   │       │       ├── V3__Extraction_tables.sql
│   │       │       ├── V4__Migration_tables.sql
│   │       │       ├── V5__Data_quality_tables.sql
│   │       │       ├── V6__Retention_tables.sql
│   │       │       ├── V7__Document_tables.sql
│   │       │       ├── V8__Compliance_tables.sql
│   │       │       └── V9__Audit_tables.sql
│   │       ├── bpmn/                      # Camunda BPMN files
│   │       │   ├── migration-workflow.bpmn
│   │       │   ├── extraction-workflow.bpmn
│   │       │   └── retention-workflow.bpmn
│   │       ├── templates/                 # Email templates
│   │       │   ├── welcome-email.html
│   │       │   ├── notification-email.html
│   │       │   └── report-email.html
│   │       └── static/                    # Static resources
│   │
│   └── test/
│       ├── java/com/jivs/platform/
│       │   ├── integration/               # Integration tests
│       │   │   ├── ExtractionServiceIT.java
│       │   │   ├── MigrationServiceIT.java
│       │   │   └── DataQualityServiceIT.java
│       │   ├── service/                   # Service tests
│       │   │   ├── ExtractionServiceTest.java
│       │   │   ├── MigrationServiceTest.java
│       │   │   └── DataQualityServiceTest.java
│       │   └── controller/                # Controller tests
│       │       ├── ExtractionControllerTest.java
│       │       ├── MigrationControllerTest.java
│       │       └── DataQualityControllerTest.java
│       └── resources/
│           ├── application-test.yml
│           └── test-data/
│
├── Dockerfile
├── .dockerignore
├── .gitignore
└── README.md
```

## Key Directories Explained

- **config/**: Spring configuration classes for security, database, cache, etc.
- **common/**: Shared utilities, exceptions, constants, and common DTOs
- **security/**: Authentication and authorization components
- **domain/**: JPA entities representing business domain
- **repository/**: Spring Data repositories for data access
- **service/**: Business logic layer with all core services
- **controller/**: REST API controllers exposing endpoints
- **dto/**: Data Transfer Objects for API requests/responses
- **mapper/**: MapStruct mappers for entity-DTO conversion
- **job/**: Quartz scheduled jobs for background processing
- **event/**: Domain events for event-driven architecture
- **listener/**: Event listeners
- **aspect/**: AOP aspects for cross-cutting concerns
- **test/**: Comprehensive test suites
