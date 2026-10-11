package com.itmanagement.servicedesk.modules.assets.model;

import java.time.Instant;
import java.time.LocalDate;

public record AssetSummary(Long id, String assetTag, String name, AssetCategory category,
                           AssetStatus status, AssetUserSummary assignedTo,
                           LocalDate warrantyExpiresOn, Instant updatedAt) {
}
