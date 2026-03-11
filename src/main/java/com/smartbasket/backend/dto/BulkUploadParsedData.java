package com.smartbasket.backend.dto;

import com.smartbasket.backend.model.Store;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Holds the fully parsed and validated result of an Excel bulk upload file.
 * Produced by {@link com.smartbasket.backend.service.BulkUploadExcelParser}.
 */
@Getter
@Builder
public class BulkUploadParsedData {

    private final int totalRows;
    private final Map<Integer, Store> storeColumns;
    private final List<ParsedRow> rows;

    /**
     * A single validated row from the Excel file, ready for persistence.
     */
    public record ParsedRow(
            int rowNumber,
            String name,
            String nameAr,
            String description,
            String descriptionAr,
            List<String> images,
            Map<String, Double> storePrices // storeId → originalPrice
    ) {}
}
