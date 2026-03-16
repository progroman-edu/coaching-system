// This DTO defines response payload fields for ReportExport endpoints.
package com.chesscoach.main.dto.report;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReportExportResponse {
    private String type;
    private String format;
    private String fileName;
    private String downloadPath;

}

