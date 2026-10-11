package com.itmanagement.servicedesk.modules.assets.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AssetDetailResponse(Long id, String assetTag, String name, AssetCategory category,
                                  String description, String manufacturer, String modelNumber,
                                  String serialNumber, LocalDate purchaseDate, BigDecimal purchaseCost,
                                  LocalDate warrantyExpiresOn, Integer usefulLifeMonths, BigDecimal salvageValue,
                                  AssetStatus status, AssetUserSummary assignedTo,
                                  Instant createdAt, Instant updatedAt,
                                  List<AssetAssignmentResponse> assignments,
                                  List<AssetMaintenanceResponse> maintenance,
                                  List<AssetDocumentResponse> documents) {
}
