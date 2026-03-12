// This DTO defines response payload fields for ReportImport endpoints.
package com.chesscoach.main.dto.report;

import java.util.List;

public class ReportImportResponse {
    private String fileName;
    private Integer totalRows;
    private Integer successRows;
    private Integer failedRows;
    private List<String> errors;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(Integer totalRows) {
        this.totalRows = totalRows;
    }

    public Integer getSuccessRows() {
        return successRows;
    }

    public void setSuccessRows(Integer successRows) {
        this.successRows = successRows;
    }

    public Integer getFailedRows() {
        return failedRows;
    }

    public void setFailedRows(Integer failedRows) {
        this.failedRows = failedRows;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}

