package com.itmanagement.servicedesk.modules.assets.model;

import jakarta.validation.constraints.NotNull;

public record AssetMaintenanceStatusRequest(@NotNull MaintenanceStatus status) {
}
