package com.jivs.platform.service.quality;

import com.jivs.platform.domain.DataQualityRule;
import com.jivs.platform.domain.DataQualityIssue;
import com.jivs.platform.domain.DataQualityProfile;
import com.jivs.platform.repository.DataQualityRuleRepository;
import com.jivs.platform.repository.DataQualityIssueRepository;
import com.jivs.platform.repository.DataQualityProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataQualityServiceTest {

    @Mock
    private DataQualityRuleRepository ruleRepository;

    @Mock
    private DataQualityIssueRepository issueRepository;

    @Mock
    private DataQualityProfileRepository profileRepository;

    @InjectMocks
    private DataQualityService dataQualityService;

    private DataQualityRule testRule;
    private DataQualityIssue testIssue;
    private DataQualityProfile testProfile;

    @BeforeEach
    void setUp() {
        testRule = new DataQualityRule();
        testRule.setId(UUID.randomUUID().toString());
        testRule.setName("Test Rule");
        testRule.setDescription("Test rule description");
        testRule.setRuleType("NULL_CHECK");
        testRule.setDimension("COMPLETENESS");
        testRule.setSeverity("HIGH");
        testRule.setEnabled(true);
        testRule.setParameters(new HashMap<>());
        testRule.setCreatedAt(new Date());
        testRule.setUpdatedAt(new Date());

        testIssue = new DataQualityIssue();
        testIssue.setId(UUID.randomUUID().toString());
        testIssue.setRuleId(testRule.getId());
        testIssue.setRuleName(testRule.getName());
        testIssue.setDimension(testRule.getDimension());
        testIssue.setSeverity(testRule.getSeverity());
        testIssue.setStatus("OPEN");
        testIssue.setDescription("Test issue");
        testIssue.setDetectedAt(new Date());

        testProfile = new DataQualityProfile();
        testProfile.setId(UUID.randomUUID().toString());
        testProfile.setDatasetName("test_dataset");
        testProfile.setTotalRecords(1000L);
        testProfile.setCompletenessScore(95.0);
        testProfile.setAccuracyScore(92.0);
        testProfile.setConsistencyScore(88.0);
        testProfile.setValidityScore(90.0);
        testProfile.setUniquenessScore(97.0);
        testProfile.setTimelinessScore(85.0);
        testProfile.setOverallScore(91.2);
        testProfile.setProfiledAt(new Date());
    }

    @Test
    void testCreateRule_Success() {
        // Arrange
        when(ruleRepository.save(any(DataQualityRule.class))).thenReturn(testRule);

        // Act
        DataQualityRule result = dataQualityService.createRule(testRule);

        // Assert
        assertNotNull(result);
        assertEquals("Test Rule", result.getName());
        assertEquals("NULL_CHECK", result.getRuleType());
        verify(ruleRepository, times(1)).save(any(DataQualityRule.class));
    }

    @Test
    void testCreateRule_NullRule_ThrowsException() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
            dataQualityService.createRule(null)
        );
        verify(ruleRepository, never()).save(any(DataQualityRule.class));
    }

    @Test
    void testGetRule_Success() {
        // Arrange
        when(ruleRepository.findById(testRule.getId()))
            .thenReturn(Optional.of(testRule));

        // Act
        Optional<DataQualityRule> result = dataQualityService.getRule(testRule.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testRule.getId(), result.get().getId());
        verify(ruleRepository, times(1)).findById(testRule.getId());
    }

    @Test
    void testGetRule_NotFound() {
        // Arrange
        String nonExistentId = UUID.randomUUID().toString();
        when(ruleRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act
        Optional<DataQualityRule> result = dataQualityService.getRule(nonExistentId);

        // Assert
        assertFalse(result.isPresent());
        verify(ruleRepository, times(1)).findById(nonExistentId);
    }

    @Test
    void testGetAllRules_Success() {
        // Arrange
        List<DataQualityRule> rules = Arrays.asList(testRule);
        Page<DataQualityRule> page = new PageImpl<>(rules);
        Pageable pageable = PageRequest.of(0, 20);

        when(ruleRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<DataQualityRule> result = dataQualityService.getAllRules(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testRule.getId(), result.getContent().get(0).getId());
        verify(ruleRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetRulesByDimension_Success() {
        // Arrange
        List<DataQualityRule> rules = Arrays.asList(testRule);
        Page<DataQualityRule> page = new PageImpl<>(rules);
        Pageable pageable = PageRequest.of(0, 20);

        when(ruleRepository.findByDimension("COMPLETENESS", pageable)).thenReturn(page);

        // Act
        Page<DataQualityRule> result = dataQualityService.getRulesByDimension("COMPLETENESS", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("COMPLETENESS", result.getContent().get(0).getDimension());
        verify(ruleRepository, times(1)).findByDimension("COMPLETENESS", pageable);
    }

    @Test
    void testUpdateRule_Success() {
        // Arrange
        when(ruleRepository.findById(testRule.getId()))
            .thenReturn(Optional.of(testRule));
        when(ruleRepository.save(any(DataQualityRule.class))).thenReturn(testRule);

        // Act
        DataQualityRule updatedRule = new DataQualityRule();
        updatedRule.setName("Updated Rule");
        DataQualityRule result = dataQualityService.updateRule(testRule.getId(), updatedRule);

        // Assert
        assertNotNull(result);
        verify(ruleRepository, times(1)).findById(testRule.getId());
        verify(ruleRepository, times(1)).save(any(DataQualityRule.class));
    }

    @Test
    void testDeleteRule_Success() {
        // Arrange
        when(ruleRepository.findById(testRule.getId()))
            .thenReturn(Optional.of(testRule));
        doNothing().when(ruleRepository).deleteById(testRule.getId());

        // Act
        dataQualityService.deleteRule(testRule.getId());

        // Assert
        verify(ruleRepository, times(1)).findById(testRule.getId());
        verify(ruleRepository, times(1)).deleteById(testRule.getId());
    }

    @Test
    void testExecuteRule_NullCheck_Success() {
        // Arrange
        testRule.setRuleType("NULL_CHECK");
        testRule.getParameters().put("field", "email");
        when(ruleRepository.findById(testRule.getId()))
            .thenReturn(Optional.of(testRule));

        Map<String, Object> data = new HashMap<>();
        data.put("email", "test@example.com");

        // Act
        Map<String, Object> result = dataQualityService.executeRule(testRule.getId(), data);

        // Assert
        assertNotNull(result);
        assertTrue(result.containsKey("passed"));
        verify(ruleRepository, times(1)).findById(testRule.getId());
    }

    @Test
    void testExecuteRule_NullCheck_Failure() {
        // Arrange
        testRule.setRuleType("NULL_CHECK");
        testRule.getParameters().put("field", "email");
        when(ruleRepository.findById(testRule.getId()))
            .thenReturn(Optional.of(testRule));
        when(issueRepository.save(any(DataQualityIssue.class))).thenReturn(testIssue);

        Map<String, Object> data = new HashMap<>();
        data.put("email", null);

        // Act
        Map<String, Object> result = dataQualityService.executeRule(testRule.getId(), data);

        // Assert
        assertNotNull(result);
        assertEquals(false, result.get("passed"));
        verify(issueRepository, times(1)).save(any(DataQualityIssue.class));
    }

    @Test
    void testExecuteRule_FormatValidation_Success() {
        // Arrange
        testRule.setRuleType("FORMAT_VALIDATION");
        testRule.getParameters().put("field", "email");
        testRule.getParameters().put("pattern", "^[A-Za-z0-9+_.-]+@(.+)$");
        when(ruleRepository.findById(testRule.getId()))
            .thenReturn(Optional.of(testRule));

        Map<String, Object> data = new HashMap<>();
        data.put("email", "test@example.com");

        // Act
        Map<String, Object> result = dataQualityService.executeRule(testRule.getId(), data);

        // Assert
        assertNotNull(result);
        assertEquals(true, result.get("passed"));
    }

    @Test
    void testGetAllIssues_Success() {
        // Arrange
        List<DataQualityIssue> issues = Arrays.asList(testIssue);
        Page<DataQualityIssue> page = new PageImpl<>(issues);
        Pageable pageable = PageRequest.of(0, 20);

        when(issueRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<DataQualityIssue> result = dataQualityService.getAllIssues(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(issueRepository, times(1)).findAll(pageable);
    }

    @Test
    void testGetIssuesByStatus_Success() {
        // Arrange
        List<DataQualityIssue> issues = Arrays.asList(testIssue);
        Page<DataQualityIssue> page = new PageImpl<>(issues);
        Pageable pageable = PageRequest.of(0, 20);

        when(issueRepository.findByStatus("OPEN", pageable)).thenReturn(page);

        // Act
        Page<DataQualityIssue> result = dataQualityService.getIssuesByStatus("OPEN", pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("OPEN", result.getContent().get(0).getStatus());
        verify(issueRepository, times(1)).findByStatus("OPEN", pageable);
    }

    @Test
    void testResolveIssue_Success() {
        // Arrange
        when(issueRepository.findById(testIssue.getId()))
            .thenReturn(Optional.of(testIssue));
        when(issueRepository.save(any(DataQualityIssue.class))).thenReturn(testIssue);

        // Act
        dataQualityService.resolveIssue(testIssue.getId(), "Fixed manually");

        // Assert
        verify(issueRepository, times(1)).findById(testIssue.getId());
        verify(issueRepository, times(1)).save(any(DataQualityIssue.class));
    }

    @Test
    void testProfileDataset_Success() {
        // Arrange
        when(profileRepository.save(any(DataQualityProfile.class))).thenReturn(testProfile);

        Map<String, Object> config = new HashMap<>();
        config.put("datasetName", "test_dataset");
        config.put("dataSource", "jdbc:postgresql://localhost:5432/test");

        // Act
        DataQualityProfile result = dataQualityService.profileDataset(config);

        // Assert
        assertNotNull(result);
        assertEquals("test_dataset", result.getDatasetName());
        verify(profileRepository, times(1)).save(any(DataQualityProfile.class));
    }

    @Test
    void testGetProfile_Success() {
        // Arrange
        when(profileRepository.findById(testProfile.getId()))
            .thenReturn(Optional.of(testProfile));

        // Act
        Optional<DataQualityProfile> result = dataQualityService.getProfile(testProfile.getId());

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testProfile.getId(), result.get().getId());
        verify(profileRepository, times(1)).findById(testProfile.getId());
    }

    @Test
    void testGetAllProfiles_Success() {
        // Arrange
        List<DataQualityProfile> profiles = Arrays.asList(testProfile);
        Page<DataQualityProfile> page = new PageImpl<>(profiles);
        Pageable pageable = PageRequest.of(0, 20);

        when(profileRepository.findAll(pageable)).thenReturn(page);

        // Act
        Page<DataQualityProfile> result = dataQualityService.getAllProfiles(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(profileRepository, times(1)).findAll(pageable);
    }

    @Test
    void testCalculateOverallScore() {
        // Test the overall score calculation
        double completeness = 95.0;
        double accuracy = 92.0;
        double consistency = 88.0;
        double validity = 90.0;
        double uniqueness = 97.0;
        double timeliness = 85.0;

        double expected = (completeness + accuracy + consistency + validity + uniqueness + timeliness) / 6.0;

        double result = dataQualityService.calculateOverallScore(
            completeness, accuracy, consistency, validity, uniqueness, timeliness
        );

        assertEquals(expected, result, 0.01);
    }

    @Test
    void testGetDashboardMetrics() {
        // Arrange
        when(ruleRepository.count()).thenReturn(10L);
        when(ruleRepository.countByEnabled(true)).thenReturn(8L);
        when(issueRepository.count()).thenReturn(25L);
        when(issueRepository.countByStatus("OPEN")).thenReturn(15L);
        when(issueRepository.countBySeverity("CRITICAL")).thenReturn(5L);

        // Act
        Map<String, Object> metrics = dataQualityService.getDashboardMetrics();

        // Assert
        assertNotNull(metrics);
        assertEquals(10L, metrics.get("totalRules"));
        assertEquals(8L, metrics.get("activeRules"));
        assertEquals(25L, metrics.get("totalIssues"));
        assertEquals(15L, metrics.get("openIssues"));
        assertEquals(5L, metrics.get("criticalIssues"));
    }

    @Test
    void testCountIssuesByDimension() {
        // Arrange
        when(issueRepository.countByDimension("COMPLETENESS")).thenReturn(10L);

        // Act
        long result = dataQualityService.countIssuesByDimension("COMPLETENESS");

        // Assert
        assertEquals(10L, result);
        verify(issueRepository, times(1)).countByDimension("COMPLETENESS");
    }

    @Test
    void testCountIssuesBySeverity() {
        // Arrange
        when(issueRepository.countBySeverity("HIGH")).thenReturn(7L);

        // Act
        long result = dataQualityService.countIssuesBySeverity("HIGH");

        // Assert
        assertEquals(7L, result);
        verify(issueRepository, times(1)).countBySeverity("HIGH");
    }

    @Test
    void testGetEnabledRules() {
        // Arrange
        List<DataQualityRule> rules = Arrays.asList(testRule);
        when(ruleRepository.findByEnabled(true)).thenReturn(rules);

        // Act
        List<DataQualityRule> result = dataQualityService.getEnabledRules();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isEnabled());
        verify(ruleRepository, times(1)).findByEnabled(true);
    }

    @Test
    void testToggleRuleEnabled() {
        // Arrange
        when(ruleRepository.findById(testRule.getId()))
            .thenReturn(Optional.of(testRule));
        when(ruleRepository.save(any(DataQualityRule.class))).thenReturn(testRule);

        // Act
        dataQualityService.toggleRuleEnabled(testRule.getId());

        // Assert
        verify(ruleRepository, times(1)).findById(testRule.getId());
        verify(ruleRepository, times(1)).save(any(DataQualityRule.class));
    }

    @Test
    void testGetRecentIssues() {
        // Arrange
        List<DataQualityIssue> issues = Arrays.asList(testIssue);
        when(issueRepository.findTop10ByOrderByDetectedAtDesc()).thenReturn(issues);

        // Act
        List<DataQualityIssue> result = dataQualityService.getRecentIssues();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(issueRepository, times(1)).findTop10ByOrderByDetectedAtDesc();
    }
}
