package com.itmanagement.servicedesk.modules.requests.model;

import com.itmanagement.servicedesk.modules.assets.model.AssetCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EquipmentRequestCreateRequest(
        @NotNull AssetCategory category,
        @NotBlank @Size(max = 140) String itemName,
        @NotBlank @Size(max = 5000) String justification) {
}
