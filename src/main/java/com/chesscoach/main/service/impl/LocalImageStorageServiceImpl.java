// This file contains project logic for LocalImageStorageServiceImpl.
package com.chesscoach.main.service.impl;

import com.chesscoach.main.service.ImageStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
}

