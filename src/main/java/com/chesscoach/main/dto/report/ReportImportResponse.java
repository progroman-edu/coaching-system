// This DTO defines response payload fields for ReportImport endpoints.
package com.chesscoach.main.dto.report;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class ReportImportResponse {
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private List<String> errors;

}

