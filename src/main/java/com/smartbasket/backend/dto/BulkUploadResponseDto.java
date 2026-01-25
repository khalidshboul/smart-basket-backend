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
    private boolean success;
    private int totalRows;
    private int successCount;
    private int errorCount;
    private String categoryId;
    private String categoryName;
    
    @Builder.Default
    private List<String> invalidStores = new ArrayList<>();
    
    @Builder.Default
    private List<RowError> errors = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RowError {
        private int row;
        private String itemName;
        private String errorType;  // VALIDATION, DUPLICATE, PRICE, STORE, SYSTEM
        private String field;      // nullable - specific field that failed
        private String message;
    }
    
    // Error type constants
    public static final String ERROR_VALIDATION = "VALIDATION";
    public static final String ERROR_DUPLICATE = "DUPLICATE";
    public static final String ERROR_PRICE = "PRICE";
    public static final String ERROR_STORE = "STORE";
    public static final String ERROR_SYSTEM = "SYSTEM";
}
