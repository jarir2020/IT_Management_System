package com.itmanagement.servicedesk.modules.assets.service;

import java.nio.file.Path;

public record AssetDownload(Path path, String originalFilename, long sizeBytes) {
}
