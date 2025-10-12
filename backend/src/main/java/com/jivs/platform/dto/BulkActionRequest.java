package com.jivs.platform.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for bulk operation requests
 * Used when performing actions on multiple entities at once
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkActionRequest {

    /**
     * List of entity IDs to perform the action on
     */
    @NotEmpty(message = "IDs list cannot be empty")
    private List<String> ids;

    /**
     * Action to perform (e.g., "start", "stop", "delete", "export")
     */
    @NotNull(message = "Action cannot be null")
    private String action;

    /**
     * Optional parameters for the action
     */
    private java.util.Map<String, Object> parameters;
}
