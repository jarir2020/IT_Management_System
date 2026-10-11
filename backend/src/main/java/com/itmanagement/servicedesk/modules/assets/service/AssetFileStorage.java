package com.itmanagement.servicedesk.modules.assets.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssetFileStorage {
    private static final Logger log = LoggerFactory.getLogger(AssetFileStorage.class);
    private static final long MAX_FILE_SIZE = 10L * 1024L * 1024L;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "png", "jpg", "jpeg", "gif", "txt", "csv", "doc", "docx", "xls", "xlsx");

    private final Path root;

    public AssetFileStorage(@Value("${app.asset-upload-dir:./var/uploads/assets}") String uploadDirectory) {
        root = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a non-empty file");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "Documents must be 10 MB or smaller");
        }
        String originalFilename = cleanFilename(file.getOriginalFilename());
        String extension = extensionOf(originalFilename);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This document type is not allowed");
        }

        String storageKey = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(root);
            try (InputStream input = file.getInputStream()) {
                Files.copy(input, resolve(storageKey));
            }
        } catch (IOException exception) {
            deleteQuietly(storageKey);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "The document could not be stored");
        }
        return new StoredFile(originalFilename, storageKey, file.getSize());
    }

    public Path pathFor(String storageKey) {
        Path path = resolve(storageKey);
        if (!Files.isRegularFile(path)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document file is missing from storage");
        }
        return path;
    }

    public void deleteQuietly(String storageKey) {
        try {
            Files.deleteIfExists(resolve(storageKey));
        } catch (IOException | ResponseStatusException exception) {
            log.warn("Could not remove stored asset document {}", storageKey, exception);
        }
    }

    private Path resolve(String storageKey) {
        if (storageKey == null || !storageKey.matches("[0-9a-fA-F-]{36}\\.[a-z0-9]{1,8}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document storage key");
        }
        Path path = root.resolve(storageKey).normalize();
        if (!path.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid document storage key");
        }
        return path;
    }

    private static String cleanFilename(String rawFilename) {
        if (rawFilename == null || rawFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file needs a filename");
        }
        String pathSafe = rawFilename.replace('\\', '/');
        String filename = pathSafe.substring(pathSafe.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "_").trim();
        if (filename.isBlank() || filename.length() > 255) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The filename must be between 1 and 255 characters");
        }
        return filename;
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 1 || dot == filename.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Attach a document with a supported extension");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String originalFilename, String storageKey, long sizeBytes) {
    }
}
