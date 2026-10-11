package com.itmanagement.servicedesk.modules.assets.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AssetMaintenanceResponse(Long id, String summary, String vendor, String notes,
                                       BigDecimal cost, LocalDate startedOn, LocalDate completedOn,
                                       MaintenanceStatus status, AssetUserSummary createdBy,
                                       Instant createdAt) {
}
