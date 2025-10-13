package com.jivs.platform.contract;

import au.com.dius.pact.provider.junit5.HttpTestTarget;
import au.com.dius.pact.provider.junit5.PactVerificationContext;
import au.com.dius.pact.provider.junit5.PactVerificationInvocationContextProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import au.com.dius.pact.provider.spring.junit5.PactVerificationSpringProvider;
import com.jivs.platform.domain.views.CustomView;
import com.jivs.platform.domain.views.ViewType;
import com.jivs.platform.domain.views.ViewConfiguration;
import com.jivs.platform.domain.views.ViewFilters;
import com.jivs.platform.domain.views.ViewColumn;
import com.jivs.platform.domain.views.ViewSorting;
import com.jivs.platform.domain.views.ViewGrouping;
import com.jivs.platform.domain.views.ViewAggregation;
import com.jivs.platform.domain.views.ChartConfig;
import com.jivs.platform.domain.views.ViewPermissions;
import com.jivs.platform.domain.views.ViewMetadata;
import com.jivs.platform.repository.CustomViewRepository;
import com.jivs.platform.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Views Contract Test - Provider Side
 *
 * This test verifies that the backend views endpoints
 * satisfy the contracts defined by the frontend.
 *
 * Custom Views are important for:
 * - User productivity (saved filters and layouts)
 * - Team collaboration (shared views)
 * - Data visualization customization
 * - Report generation
 * - Workflow optimization
 *
 * Coverage: 2/2 Views endpoints
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Provider("JiVS Backend")
@PactFolder("../frontend/pacts")
@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
public class ViewsContractTest {

    @LocalServerPort
    private int port;

    @Autowired
    private CustomViewRepository viewRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setup(PactVerificationContext context) {
        // Configure the test to hit our running Spring Boot application
        context.setTarget(new HttpTestTarget("localhost", port));

        // Mock JWT validation for tests
        when(jwtTokenProvider.validateToken(anyString())).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(anyString())).thenReturn(1L);
        when(jwtTokenProvider.getRolesFromToken(anyString())).thenReturn(Collections.singletonList("ROLE_USER"));
    }

    @TestTemplate
    @ExtendWith(PactVerificationInvocationContextProvider.class)
    void pactVerificationTestTemplate(PactVerificationContext context) {
        // This will run once for each interaction in the pact file
        context.verifyInteraction();
    }

    /**
     * Provider States - Set up test data for different views scenarios
     */

    @State("user is authenticated")
    public void userIsAuthenticated() {
        System.out.println("Setting up: User is authenticated");
        // Authentication is mocked in setup()
    }

    @State("user is authenticated and has custom views")
    public void userAuthenticatedWithCustomViews() {
        System.out.println("Setting up: User is authenticated and has custom views");

        // Clear existing data
        viewRepository.deleteAll();

        // Create test views
        for (int i = 1; i <= 3; i++) {
            CustomView view = new CustomView();
            view.setId("VIEW-00" + i);
            view.setName(i == 1 ? "My Extraction Dashboard" :
                       i == 2 ? "Migration Monitor" :
                       "Quality Overview");
            view.setDescription(i == 1 ? "Custom view for extraction monitoring" :
                               i == 2 ? "Real-time migration tracking" :
                               "Data quality metrics dashboard");
            view.setType(i == 1 ? ViewType.EXTRACTION :
                        i == 2 ? ViewType.MIGRATION :
                        ViewType.DATA_QUALITY);
            view.setUserId("USER-001");
            view.setIsPublic(i == 3);
            view.setIsDefault(i == 1);

            // View configuration
            ViewConfiguration config = new ViewConfiguration();

            // Filters
            ViewFilters filters = new ViewFilters();
            if (i == 1) {
                filters.setStatus(Arrays.asList("RUNNING", "COMPLETED"));
                filters.setSourceType(Arrays.asList("JDBC", "SAP"));
            } else if (i == 2) {
                filters.setStatus(Arrays.asList("RUNNING"));
            }
            filters.setDateRange(LocalDateTime.now().minusDays(7), LocalDateTime.now());
            config.setFilters(filters);

            // Columns
            List<ViewColumn> columns = new ArrayList<>();
            String[] fieldNames = {"name", "status", "createdAt", "recordsProcessed", "progress"};
            for (int j = 0; j < 5; j++) {
                ViewColumn column = new ViewColumn();
                column.setField(fieldNames[j]);
                column.setLabel(fieldNames[j].substring(0, 1).toUpperCase() + fieldNames[j].substring(1));
                column.setVisible(true);
                column.setWidth(j == 0 ? 300 : 150);
                column.setSortable(j != 4);
                column.setOrder(j + 1);
                columns.add(column);
            }
            config.setColumns(columns);

            // Sorting
            ViewSorting sorting = new ViewSorting();
            sorting.setField("createdAt");
            sorting.setOrder("DESC");
            config.setSorting(sorting);

            // Grouping
            ViewGrouping grouping = new ViewGrouping();
            grouping.setEnabled(i == 2);
            grouping.setField(i == 2 ? "phase" : "sourceType");
            config.setGrouping(grouping);

            // Aggregations
            if (i == 1 || i == 2) {
                List<ViewAggregation> aggregations = new ArrayList<>();
                ViewAggregation agg1 = new ViewAggregation();
                agg1.setField("recordsExtracted");
                agg1.setFunction("SUM");
                agg1.setLabel("Total Records");
                aggregations.add(agg1);
                config.setAggregations(aggregations);
            }

            // Chart configuration
            ChartConfig chartConfig = new ChartConfig();
            chartConfig.setType(i == 1 ? "line" : i == 2 ? "bar" : "pie");
            chartConfig.setXAxis("date");
            chartConfig.setYAxis(i == 1 ? "count" : i == 2 ? "progress" : "score");
            chartConfig.setShowLegend(true);
            chartConfig.setShowGrid(true);
            config.setChartConfig(chartConfig);

            view.setConfiguration(config);

            // Permissions
            ViewPermissions permissions = new ViewPermissions();
            permissions.setCanEdit(true);
            permissions.setCanDelete(true);
            permissions.setCanShare(i != 3);
            permissions.setCanDuplicate(true);
            view.setPermissions(permissions);

            // Metadata
            ViewMetadata metadata = new ViewMetadata();
            metadata.setCreatedBy("USER-001");
            metadata.setCreatedByName("John Doe");
            metadata.setCreatedAt(LocalDateTime.now().minusDays(30 - i * 5));
            metadata.setUpdatedAt(LocalDateTime.now().minusDays(i));
            metadata.setLastAccessedAt(LocalDateTime.now().minusHours(i * 2));
            metadata.setAccessCount(42 + i * 10);
            if (i == 3) {
                metadata.setSharedWith(Arrays.asList("USER-002", "USER-003"));
            }
            view.setMetadata(metadata);

            // Tags
            if (i == 1) {
                view.setTags(Arrays.asList("extraction", "monitoring"));
            } else if (i == 2) {
                view.setTags(Arrays.asList("migration", "real-time"));
            } else {
                view.setTags(Arrays.asList("quality", "dashboard"));
            }

            viewRepository.save(view);
        }
    }

    @State("user is authenticated and can create views")
    public void userAuthenticatedCanCreateViews() {
        System.out.println("Setting up: User is authenticated and can create views");

        // Clear existing data
        viewRepository.deleteAll();

        // The user can create views - no specific setup needed
        // The controller will handle view creation
    }
}

/**
 * WHY VIEWS CONTRACT TESTS ARE ESSENTIAL:
 *
 * 1. Custom views save significant user time (no repetitive filtering)
 * 2. Shared views ensure team consistency
 * 3. View configurations must persist accurately
 * 4. Column preferences affect data visibility
 * 5. Aggregations provide critical business insights
 *
 * These tests ensure:
 * - Frontend and backend agree on view structure
 * - Filter logic is consistent
 * - Column configuration persists correctly
 * - Aggregation functions match
 * - Permission model is enforced
 *
 * Benefits:
 * - Catch view configuration mismatches
 * - Ensure filter persistence
 * - Validate column ordering
 * - Confirm aggregation accuracy
 * - Maintain team collaboration features
 *
 * Real productivity impact:
 * - Saved views eliminate 5-10 minutes per session
 * - Shared views ensure team alignment
 * - Custom columns show relevant data immediately
 * - Aggregations provide instant insights
 */