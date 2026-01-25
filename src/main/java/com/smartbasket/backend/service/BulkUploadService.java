package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.BulkUploadResponseDto;
import com.smartbasket.backend.dto.BulkUploadResponseDto.RowError;
import com.smartbasket.backend.exception.BulkUploadValidationException;
import com.smartbasket.backend.exception.ResourceNotFoundException;
import com.smartbasket.backend.model.Category;
import com.smartbasket.backend.model.ReferenceItem;
import com.smartbasket.backend.model.Store;
import com.smartbasket.backend.model.StoreItem;
import com.smartbasket.backend.repository.CategoryRepository;
import com.smartbasket.backend.repository.ReferenceItemRepository;
import com.smartbasket.backend.repository.StoreItemRepository;
import com.smartbasket.backend.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkUploadService {
    
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ReferenceItemRepository referenceItemRepository;
    private final StoreItemRepository storeItemRepository;
    
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
     * Internal DTO to hold parsed row data before validation
     */
    private record ParsedRow(
            int rowNumber,
            String name,
            String nameAr,
            String description,
            String descriptionAr,
            List<String> images,
            Map<String, Double> storePrices // storeId -> originalPrice
    ) {}
    
    @Transactional(rollbackFor = Exception.class)
    public BulkUploadResponseDto processExcelFile(MultipartFile file, String categoryId) throws IOException {
        // Phase 1: Validate category exists
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Phase 2: Parse header row and validate stores
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new BulkUploadValidationException(
                        0, categoryId, category.getName(),
                        List.of(RowError.builder()
                                .row(1)
                                .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                                .message("Excel file is empty or missing header row")
                                .build())
                );
            }
            
            // Extract store names from header (columns 7+)
            Map<Integer, Store> storeColumns = new LinkedHashMap<>();
            List<String> invalidStores = new ArrayList<>();
            List<RowError> errors = new ArrayList<>();
            
            for (int col = FIRST_STORE_COL; col < headerRow.getLastCellNum(); col++) {
                Cell cell = headerRow.getCell(col);
                String storeName = getStringCellValue(cell);
                if (storeName != null && !storeName.isBlank()) {
                    Optional<Store> storeOpt = findStoreByName(storeName);
                    if (storeOpt.isPresent()) {
                        storeColumns.put(col, storeOpt.get());
                    } else {
                        invalidStores.add(storeName);
                        errors.add(RowError.builder()
                                .row(1)
                                .errorType(BulkUploadResponseDto.ERROR_STORE)
                                .field(storeName)
                                .message("Store not found: " + storeName)
                                .build());
                    }
                }
            }
            
            // Fail if any stores are invalid
            if (!invalidStores.isEmpty()) {
                throw new BulkUploadValidationException(0, categoryId, category.getName(), errors, invalidStores);
            }
            
            if (storeColumns.isEmpty()) {
                throw new BulkUploadValidationException(
                        0, categoryId, category.getName(),
                        List.of(RowError.builder()
                                .row(1)
                                .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                                .message("No valid store columns found in header row (columns H onwards)")
                                .build())
                );
            }
            
            // Phase 3: Parse and validate all rows
            List<ParsedRow> parsedRows = new ArrayList<>();
            int totalRows = 0;
            
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                
                totalRows++;
                int excelRowNum = rowNum + 1; // 1-indexed for user display
                
                // Parse row data
                String name = getStringCellValue(row.getCell(COL_NAME));
                String nameAr = getStringCellValue(row.getCell(COL_NAME_AR));
                String description = getStringCellValue(row.getCell(COL_DESCRIPTION));
                String descriptionAr = getStringCellValue(row.getCell(COL_DESCRIPTION_AR));
                String image1 = getStringCellValue(row.getCell(COL_IMAGE1));
                String image2 = getStringCellValue(row.getCell(COL_IMAGE2));
                String image3 = getStringCellValue(row.getCell(COL_IMAGE3));
                
                // Validate required fields
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
                
                // Build images list
                List<String> images = new ArrayList<>();
                if (image1 != null && !image1.isBlank()) images.add(image1);
                if (image2 != null && !image2.isBlank()) images.add(image2);
                if (image3 != null && !image3.isBlank()) images.add(image3);
                
                // Parse store prices
                Map<String, Double> storePrices = new HashMap<>();
                boolean hasPriceError = false;
                
                for (Map.Entry<Integer, Store> entry : storeColumns.entrySet()) {
                    Cell priceCell = row.getCell(entry.getKey());
                    String rawValue = getStringCellValue(priceCell);
                    
                    if (rawValue != null && !rawValue.isBlank()) {
                        Double price = getNumericCellValue(priceCell);
                        if (price == null) {
                            errors.add(RowError.builder()
                                    .row(excelRowNum)
                                    .itemName(name)
                                    .errorType(BulkUploadResponseDto.ERROR_PRICE)
                                    .field(entry.getValue().getName())
                                    .message("Invalid price value: '" + rawValue + "'")
                                    .build());
                            hasPriceError = true;
                        } else if (price > 0) {
                            storePrices.put(entry.getValue().getId(), price);
                        }
                    }
                }
                
                if (hasPriceError) {
                    continue;
                }
                
                // At least one store price is required
                if (storePrices.isEmpty()) {
                    errors.add(RowError.builder()
                            .row(excelRowNum)
                            .itemName(name)
                            .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                            .message("At least one store price is required")
                            .build());
                    continue;
                }
                
                parsedRows.add(new ParsedRow(excelRowNum, name, nameAr, description, descriptionAr, images, storePrices));
            }
            
            // Phase 4: Fail if any validation errors
            if (!errors.isEmpty()) {
                throw new BulkUploadValidationException(totalRows, categoryId, category.getName(), errors);
            }
            
            if (parsedRows.isEmpty()) {
                throw new BulkUploadValidationException(
                        0, categoryId, category.getName(),
                        List.of(RowError.builder()
                                .row(0)
                                .errorType(BulkUploadResponseDto.ERROR_VALIDATION)
                                .message("No valid data rows found in the Excel file")
                                .build())
                );
            }
            
            // Phase 5: Write all data (inside transaction)
            int successCount = 0;
            
            for (ParsedRow parsedRow : parsedRows) {
                // Create or update ReferenceItem
                ReferenceItem referenceItem = createOrUpdateReferenceItem(
                        parsedRow.name(),
                        parsedRow.nameAr(),
                        category,
                        parsedRow.description(),
                        parsedRow.descriptionAr(),
                        parsedRow.images(),
                        new ArrayList<>(parsedRow.storePrices().keySet())
                );
                
                // Create StoreItems for each store price
                for (Map.Entry<String, Double> priceEntry : parsedRow.storePrices().entrySet()) {
                    createOrUpdateStoreItem(referenceItem, priceEntry.getKey(), priceEntry.getValue());
                }
                
                successCount++;
            }
            
            log.info("Bulk upload completed: {} items processed for category '{}'", successCount, category.getName());
            
            return BulkUploadResponseDto.builder()
                    .success(true)
                    .totalRows(totalRows)
                    .successCount(successCount)
                    .errorCount(0)
                    .categoryId(categoryId)
                    .categoryName(category.getName())
                    .build();
        }
    }
    
    private ReferenceItem createOrUpdateReferenceItem(
            String name, String nameAr, Category category,
            String description, String descriptionAr, List<String> images, List<String> storeIds
    ) {
        // Check if item already exists by name
        Optional<ReferenceItem> existingItem = referenceItemRepository.findByNameIgnoreCase(name);
        
        if (existingItem.isPresent()) {
            ReferenceItem item = existingItem.get();
            // Update fields if they were empty
            if ((item.getNameAr() == null || item.getNameAr().isBlank()) && nameAr != null) {
                item.setNameAr(nameAr);
            }
            if ((item.getDescription() == null || item.getDescription().isBlank()) && description != null) {
                item.setDescription(description);
            }
            if ((item.getDescriptionAr() == null || item.getDescriptionAr().isBlank()) && descriptionAr != null) {
                item.setDescriptionAr(descriptionAr);
            }
            if ((item.getImages() == null || item.getImages().isEmpty()) && !images.isEmpty()) {
                item.setImages(images);
            }
            // Add stores to specificStoreIds if not already present
            List<String> currentStoreIds = item.getSpecificStoreIds();
            if (currentStoreIds == null) {
                currentStoreIds = new ArrayList<>();
            }
            for (String storeId : storeIds) {
                if (!currentStoreIds.contains(storeId)) {
                    currentStoreIds.add(storeId);
                }
            }
            item.setSpecificStoreIds(currentStoreIds);
            return referenceItemRepository.save(item);
        }
        
        // Create new reference item
        ReferenceItem newItem = ReferenceItem.builder()
                .name(name)
                .nameAr(nameAr)
                .categoryId(category.getId())
                .category(category.getName())
                .description(description)
                .descriptionAr(descriptionAr)
                .images(images)
                .active(true)
                .availableInAllStores(false)
                .specificStoreIds(storeIds)
                .build();
        
        return referenceItemRepository.save(newItem);
    }
    
    private void createOrUpdateStoreItem(ReferenceItem referenceItem, String storeId, Double originalPrice) {
        Optional<StoreItem> existingStoreItem = storeItemRepository
                .findByStoreIdAndReferenceItemId(storeId, referenceItem.getId());
        
        if (existingStoreItem.isPresent()) {
            StoreItem storeItem = existingStoreItem.get();
            storeItem.setOriginalPrice(originalPrice);
            storeItem.setLastPriceUpdate(Instant.now());
            storeItemRepository.save(storeItem);
        } else {
            StoreItem newStoreItem = StoreItem.builder()
                    .storeId(storeId)
                    .referenceItemId(referenceItem.getId())
                    .name(referenceItem.getName())
                    .nameAr(referenceItem.getNameAr())
                    .images(referenceItem.getImages())
                    .originalPrice(originalPrice)
                    .currency("JOD")
                    .lastPriceUpdate(Instant.now())
                    .build();
            storeItemRepository.save(newStoreItem);
        }
    }
    
    private Optional<Store> findStoreByName(String name) {
        // Try English name first
        Optional<Store> store = storeRepository.findByNameIgnoreCase(name);
        if (store.isPresent()) {
            return store;
        }
        // Try Arabic name
        return storeRepository.findByNameArIgnoreCase(name);
    }
    
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
}
