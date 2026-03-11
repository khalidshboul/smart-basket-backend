package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.BulkUploadParsedData;
import com.smartbasket.backend.dto.BulkUploadParsedData.ParsedRow;
import com.smartbasket.backend.dto.BulkUploadResponseDto;
import com.smartbasket.backend.dto.BulkUploadResponseDto.RowError;
import com.smartbasket.backend.exception.BulkUploadValidationException;
import com.smartbasket.backend.model.Store;
import com.smartbasket.backend.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * Responsible for parsing and validating an Excel file for bulk upload.
 * Produces a {@link BulkUploadParsedData} that is ready for persistence.
 */
@Component
@RequiredArgsConstructor
public class BulkUploadExcelParser {

    private final StoreRepository storeRepository;

    // Fixed column indices (0-based)
    private static final int COL_NAME = 0;
    private static final int COL_NAME_AR = 1;
    private static final int COL_DESCRIPTION = 2;
    private static final int COL_DESCRIPTION_AR = 3;
    private static final int COL_IMAGE1 = 4;
    private static final int COL_IMAGE2 = 5;
    private static final int COL_IMAGE3 = 6;
    private static final int FIRST_STORE_COL = 7;

    /**
     * Parses and validates an Excel file, returning structured data ready for persistence.
     *
     * @throws BulkUploadValidationException if header, stores, or rows fail validation
     * @throws IOException                   if the file cannot be read
     */
    public BulkUploadParsedData parse(MultipartFile file, String categoryId, String categoryName) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            Row headerRow = validateHeaderRow(sheet, categoryId, categoryName);
            Map<Integer, Store> storeColumns = parseStoreColumns(headerRow, categoryId, categoryName);
            return parseDataRows(sheet, storeColumns, categoryId, categoryName);
        }
    }

    // ──────────────────────────────────────────────
    // Header & Store Validation
    // ──────────────────────────────────────────────

    private Row validateHeaderRow(Sheet sheet, String categoryId, String categoryName) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new BulkUploadValidationException(
                    0, categoryId, categoryName,
                    List.of(RowError.builder()
                            .row(1)
                            .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                            .message("Excel file is empty or missing header row")
                            .build())
            );
        }
        return headerRow;
    }

    private Map<Integer, Store> parseStoreColumns(Row headerRow, String categoryId, String categoryName) {
        Map<Integer, Store> storeColumns = new LinkedHashMap<>();
        List<String> invalidStores = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();

        for (int col = FIRST_STORE_COL; col < headerRow.getLastCellNum(); col++) {
            String storeName = getStringCellValue(headerRow.getCell(col));
            if (storeName == null || storeName.isBlank()) {
                continue;
            }

            final int colIndex = col;
            findStoreByName(storeName).ifPresentOrElse(
                    store -> storeColumns.put(colIndex, store),
                    () -> {
                        invalidStores.add(storeName);
                        errors.add(RowError.builder()
                                .row(1)
                                .errorType(BulkUploadResponseDto.ERROR_STORE)
                                .field(storeName)
                                .message("Store not found: " + storeName)
                                .build());
                    }
            );
        }

        if (!invalidStores.isEmpty()) {
            throw new BulkUploadValidationException(0, categoryId, categoryName, errors, invalidStores);
        }
        return storeColumns;
    }

    // ──────────────────────────────────────────────
    // Data Row Parsing
    // ──────────────────────────────────────────────

    private BulkUploadParsedData parseDataRows(
            Sheet sheet, Map<Integer, Store> storeColumns,
            String categoryId, String categoryName) {

        List<ParsedRow> parsedRows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        int totalRows = 0;

        for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null || isRowEmpty(row)) {
                continue;
            }
            totalRows++;
            int excelRowNum = rowNum + 1; // 1-indexed for user display

            String name = getStringCellValue(row.getCell(COL_NAME));
            if (name == null || name.isBlank()) {
                errors.add(RowError.builder()
                        .row(excelRowNum)
                        .itemName(name)
                        .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                        .field("name")
                        .message("Item name is required")
                        .build());
                continue;
            }

            parsedRows.add(new ParsedRow(
                    excelRowNum,
                    name,
                    getStringCellValue(row.getCell(COL_NAME_AR)),
                    getStringCellValue(row.getCell(COL_DESCRIPTION)),
                    getStringCellValue(row.getCell(COL_DESCRIPTION_AR)),
                    parseImages(row),
                    parseStorePrices(row, storeColumns)
            ));
        }

        if (!errors.isEmpty()) {
            throw new BulkUploadValidationException(totalRows, categoryId, categoryName, errors);
        }
        if (parsedRows.isEmpty()) {
            throw new BulkUploadValidationException(
                    0, categoryId, categoryName,
                    List.of(RowError.builder()
                            .row(0)
                            .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                            .message("No valid data rows found in the Excel file")
                            .build())
            );
        }

        return BulkUploadParsedData.builder()
                .totalRows(totalRows)
                .storeColumns(storeColumns)
                .rows(parsedRows)
                .build();
    }

    private List<String> parseImages(Row row) {
        List<String> images = new ArrayList<>();
        addIfNotBlank(images, getStringCellValue(row.getCell(COL_IMAGE1)));
        addIfNotBlank(images, getStringCellValue(row.getCell(COL_IMAGE2)));
        addIfNotBlank(images, getStringCellValue(row.getCell(COL_IMAGE3)));
        return images;
    }

    private Map<String, Double> parseStorePrices(Row row, Map<Integer, Store> storeColumns) {
        Map<String, Double> storePrices = new HashMap<>();
        for (Map.Entry<Integer, Store> entry : storeColumns.entrySet()) {
            Cell priceCell = row.getCell(entry.getKey());
            String rawValue = getStringCellValue(priceCell);

            if (rawValue != null && !rawValue.isBlank()) {
                Double price = getNumericCellValue(priceCell);
                if (price != null && price > 0) {
                    storePrices.put(entry.getValue().getId(), price);
                }
            }
        }
        return storePrices;
    }

    // ──────────────────────────────────────────────
    // Store Lookup
    // ──────────────────────────────────────────────

    private Optional<Store> findStoreByName(String name) {
        return storeRepository.findByNameIgnoreCase(name)
                .or(() -> storeRepository.findByNameArIgnoreCase(name));
    }

    // ──────────────────────────────────────────────
    // Cell Utilities
    // ──────────────────────────────────────────────

    private String getStringCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Double getNumericCellValue(Cell cell) {
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                String value = cell.getStringCellValue().trim();
                if (value.isBlank()) {
                    yield null;
                }
                try {
                    yield Double.parseDouble(value);
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
            default -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getStringCellValue(cell);
                if (value != null && !value.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void addIfNotBlank(List<String> list, String value) {
        if (value != null && !value.isBlank()) {
            list.add(value);
        }
    }
}
