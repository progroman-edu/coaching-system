// This service implementation contains business logic for LocalImageStorage operations.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;

@Service
public class LocalImageStorageServiceImpl implements ImageStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    @Value("${app.upload.base-dir:uploads}")
    private String uploadBaseDir;

    @Value("${app.upload.trainee-photo-subdir:trainee-photos}")
    private String traineePhotoSubdir;

    @Override
    public String saveTraineePhoto(Long traineeId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is required");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "";
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Unsupported image format. Allowed: jpg, jpeg, png, webp");
        }

        String detectedType = detectImageType(file);
        if (detectedType == null) {
            throw new IllegalArgumentException("File signature does not match a supported image type");
        }
        if (!matchesExtension(extension, detectedType)) {
            throw new IllegalArgumentException("File extension does not match image content");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("Uploaded file must be an image");
        }

        String fileName = "trainee-" + traineeId + "-" + OffsetDateTime.now().toEpochSecond() + "." + extension;
        Path storageDir = Paths.get(uploadBaseDir, traineePhotoSubdir).toAbsolutePath().normalize();
        Path target = storageDir.resolve(fileName);

        try {
            Files.createDirectories(storageDir);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to store image: " + ex.getMessage(), ex);
        }

        return "/uploads/" + traineePhotoSubdir + "/" + fileName;
    }

    private String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String detectImageType(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(12);
            if (header.length >= 3
                && (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF) {
                return "jpeg";
            }
            if (header.length >= 8
                && (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A) {
                return "png";
            }
            if (header.length >= 12
                && header[0] == 0x52
                && header[1] == 0x49
                && header[2] == 0x46
                && header[3] == 0x46
                && header[8] == 0x57
                && header[9] == 0x45
                && header[10] == 0x42
                && header[11] == 0x50) {
                return "webp";
            }
            return null;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to inspect uploaded image: " + ex.getMessage(), ex);
        }
    }

    private boolean matchesExtension(String extension, String detectedType) {
        if ("jpeg".equals(detectedType)) {
            return "jpg".equals(extension) || "jpeg".equals(extension);
        }
        return extension.equals(detectedType);
    }
}

