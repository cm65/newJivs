package com.jivs.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for saved views.
 * Part of Sprint 2 - Workflow 7: Advanced Filtering Implementation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedViewDTO {

    private Long id;

    @NotBlank(message = "Module cannot be blank")
    @Pattern(regexp = "^(extractions|migrations|data-quality|compliance)$",
            message = "Module must be: extractions, migrations, data-quality, or compliance")
    private String module;

    @NotBlank(message = "View name cannot be blank")
    @Size(min = 1, max = 100, message = "View name must be between 1 and 100 characters")
    private String viewName;

    private Boolean isDefault;

    private Map<String, Object> filters;

    private Map<String, String> sorting;

    private List<String> visibleColumns;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
