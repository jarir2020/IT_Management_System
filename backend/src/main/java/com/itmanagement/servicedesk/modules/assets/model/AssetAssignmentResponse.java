package com.itmanagement.servicedesk.modules.assets.model;

import java.time.Instant;

public record AssetAssignmentResponse(Long id, AssetUserSummary assignedTo, AssetUserSummary assignedBy,
                                      AssetUserSummary returnedBy, String assignmentNote,
                                      String returnCondition, Instant assignedAt, Instant returnedAt,
                                      Long equipmentRequestId) {
}
