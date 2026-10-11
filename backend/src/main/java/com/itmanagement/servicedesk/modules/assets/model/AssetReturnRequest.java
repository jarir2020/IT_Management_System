package com.itmanagement.servicedesk.modules.assets.model;

import jakarta.validation.constraints.Size;

public record AssetReturnRequest(
        @Size(max = 2000) String conditionNotes,
        boolean needsMaintenance) {
}
