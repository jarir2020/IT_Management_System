package com.itmanagement.servicedesk.modules.assets.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AssetAssignmentRequest(
        @NotNull Long userId,
        @Size(max = 1000) String note) {
}
