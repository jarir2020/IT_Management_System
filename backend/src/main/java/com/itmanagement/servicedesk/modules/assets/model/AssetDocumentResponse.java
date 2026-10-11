package com.itmanagement.servicedesk.modules.assets.model;

import java.time.Instant;

public record AssetDocumentResponse(Long id, AssetDocumentType type, String originalFilename,
                                    long sizeBytes, AssetUserSummary uploadedBy, Instant uploadedAt) {
}
