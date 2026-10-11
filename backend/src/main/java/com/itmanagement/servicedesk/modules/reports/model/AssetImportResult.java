package com.itmanagement.servicedesk.modules.reports.model;

import java.util.List;

public record AssetImportResult(int scannedRows, int importedRows, int skippedRows,
                                List<AssetImportIssue> issues) {
}
