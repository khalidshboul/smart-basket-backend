package com.smartbasket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponseDto {
    private int totalSheets;
    private int totalRows;
    private int successCount;
    private int errorCount;
    
    @Builder.Default
    private List<SheetResult> sheetResults = new ArrayList<>();
    
    @Builder.Default
    private List<UploadError> errors = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SheetResult {
        private String sheetName;
        private String storeName;
        private int rowsProcessed;
        private int successCount;
        private int errorCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UploadError {
        private String sheetName;
        private int rowNumber;
        private String itemName;
        private String errorMessage;
    }
}
