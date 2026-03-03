// This file contains project logic for ReportService.
package com.chesscoach.main.service;

import com.chesscoach.main.dto.report.ReportExportResponse;
import com.chesscoach.main.dto.report.ReportImportResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ReportService {
    ReportExportResponse export(String type, String format);

    ReportImportResponse importTrainees(MultipartFile file);
}

