package com.chesscoach.main.controller;

import com.chesscoach.main.config.ApiPaths;
import com.chesscoach.main.dto.common.ApiResponse;
import com.chesscoach.main.dto.report.ReportExportResponse;
import com.chesscoach.main.dto.report.ReportImportResponse;
import com.chesscoach.main.service.ReportService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping(ApiPaths.REPORTS)
public class ReportController {

    private final ReportService reportService;

    @Value("${app.report.export-dir:uploads/reports}")
    private String exportDir;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/export")
    public ResponseEntity<ApiResponse<ReportExportResponse>> export(
        @RequestParam String type,
        @RequestParam String format,
        HttpServletRequest request
    ) {
        ReportExportResponse data = reportService.export(type, format);
        return ResponseEntity.ok(ApiResponse.ok("Export prepared", data, request.getRequestURI()));
    }

    @PostMapping("/import/trainees")
    public ResponseEntity<ApiResponse<ReportImportResponse>> importTrainees(
        @RequestParam("file") MultipartFile file,
        HttpServletRequest request
    ) {
        ReportImportResponse data = reportService.importTrainees(file);
        return ResponseEntity.ok(ApiResponse.ok("Import received", data, request.getRequestURI()));
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> download(@PathVariable String fileName) throws MalformedURLException {
        Path file = Paths.get(exportDir).toAbsolutePath().normalize().resolve(fileName).normalize();
        Path base = Paths.get(exportDir).toAbsolutePath().normalize();
        if (!file.startsWith(base)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        Resource resource = new UrlResource(file.toUri());
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .body(resource);
    }
}
