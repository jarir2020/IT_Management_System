package com.itmanagement.servicedesk.modules.requests.model;

import com.itmanagement.servicedesk.modules.assets.model.AssetCategory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record EquipmentRequestResponse(Long id, AssetCategory category, String itemName,
        String justification, EquipmentRequestStatus status, RequestUserSummary requester,
        Long departmentId, String departmentName, String decisionNote, RequestUserSummary reviewedBy,
        Instant decidedAt, String procurementVendor, String purchaseReference, BigDecimal purchaseCost,
        LocalDate expectedDeliveryDate, String procurementNotes, RequestUserSummary procurementUpdatedBy,
        Instant receivedAt, Long allocatedAssetId, Instant fulfilledAt, Instant createdAt, Instant updatedAt) {
}
