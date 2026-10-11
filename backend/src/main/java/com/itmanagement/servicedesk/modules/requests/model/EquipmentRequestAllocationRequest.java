package com.itmanagement.servicedesk.modules.requests.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipmentRequestAllocationRequest(
        @NotNull Long assetId,
        @Size(max = 1000) String note) {
}
