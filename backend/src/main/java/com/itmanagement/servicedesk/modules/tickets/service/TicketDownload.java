package com.itmanagement.servicedesk.modules.tickets.service;

import java.nio.file.Path;

public record TicketDownload(Path path, String originalFilename, long sizeBytes) {
}
