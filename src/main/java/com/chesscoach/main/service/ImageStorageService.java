// This service interface defines operations for ImageStorage workflows.
package com.chesscoach.main.service;

import org.springframework.web.multipart.MultipartFile;

public interface ImageStorageService {
    String saveTraineePhoto(Long traineeId, MultipartFile file);
}

