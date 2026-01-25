package com.smartbasket.backend.exception;

import com.smartbasket.backend.dto.BulkUploadResponseDto.RowError;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class BulkUploadValidationException extends RuntimeException {
    private final int totalRows;
    private final String categoryId;
    private final String categoryName;
    private final List<RowError> errors;
    private final List<String> invalidStores;
    
    public BulkUploadValidationException(
            int totalRows,
            String categoryId,
            String categoryName,
            List<RowError> errors,
            List<String> invalidStores
    ) {
        super("Bulk upload validation failed with " + errors.size() + " errors");
        this.totalRows = totalRows;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.errors = errors != null ? errors : new ArrayList<>();
        this.invalidStores = invalidStores != null ? invalidStores : new ArrayList<>();
    }
    
    public BulkUploadValidationException(int totalRows, String categoryId, String categoryName, List<RowError> errors) {
        this(totalRows, categoryId, categoryName, errors, new ArrayList<>());
    }
}
