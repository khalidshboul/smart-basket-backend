package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.BulkUploadResponseDto;
import com.smartbasket.backend.dto.BulkUploadResponseDto.SheetResult;
import com.smartbasket.backend.dto.BulkUploadResponseDto.UploadError;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkUploadService {
    
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final ReferenceItemRepository referenceItemRepository;
    private final StoreItemRepository storeItemRepository;
    
    // Column indices (0-based)
    private static final int COL_NAME = 0;
    private static final int COL_NAME_AR = 1;
    private static final int COL_CATEGORY = 2;
    private static final int COL_ORIGINAL_PRICE = 3;
    private static final int COL_DISCOUNT_PRICE = 4;
    private static final int COL_DESCRIPTION = 5;
    private static final int COL_DESCRIPTION_AR = 6;
    private static final int COL_IMAGE1 = 7;
    private static final int COL_IMAGE2 = 8;
    private static final int COL_IMAGE3 = 9;
    
    @Transactional
    public BulkUploadResponseDto processExcelFile(MultipartFile file) throws IOException {
        List<SheetResult> sheetResults = new ArrayList<>();
        List<UploadError> errors = new ArrayList<>();
        int totalRows = 0;
        int totalSuccess = 0;
        int totalErrors = 0;
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            int numberOfSheets = workbook.getNumberOfSheets();
            
            for (int i = 0; i < numberOfSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                
                // Lookup store by sheet name (EN or AR)
                Optional<Store> storeOpt = findStoreByName(sheetName);
                if (storeOpt.isEmpty()) {
                    errors.add(UploadError.builder()
                            .sheetName(sheetName)
                            .rowNumber(0)
                            .errorMessage("Store not found with name: " + sheetName)
                            .build());
                    sheetResults.add(SheetResult.builder()
                            .sheetName(sheetName)
                            .rowsProcessed(0)
                            .successCount(0)
                            .errorCount(1)
                            .build());
                    totalErrors++;
                    continue;
                }
                
                Store store = storeOpt.get();
                SheetResult sheetResult = processSheet(sheet, store, errors);
                sheetResults.add(sheetResult);
                
                totalRows += sheetResult.getRowsProcessed();
                totalSuccess += sheetResult.getSuccessCount();
                totalErrors += sheetResult.getErrorCount();
            }
        }
        
        return BulkUploadResponseDto.builder()
                .totalSheets(sheetResults.size())
                .totalRows(totalRows)
                .successCount(totalSuccess)
                .errorCount(totalErrors)
                .sheetResults(sheetResults)
                .errors(errors)
                .build();
    }
    
    private SheetResult processSheet(Sheet sheet, Store store, List<UploadError> errors) {
        String sheetName = sheet.getSheetName();
        int rowsProcessed = 0;
        int successCount = 0;
        int errorCount = 0;
        
        // Skip header row, start from row 1
        for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row == null || isRowEmpty(row)) {
                continue;
            }
            
            rowsProcessed++;
            
            try {
                processRow(row, store, sheetName, errors);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                String itemName = getStringCellValue(row.getCell(COL_NAME));
                errors.add(UploadError.builder()
                        .sheetName(sheetName)
                        .rowNumber(rowNum + 1)
                        .itemName(itemName)
                        .errorMessage(e.getMessage())
                        .build());
                log.error("Error processing row {} in sheet {}: {}", rowNum + 1, sheetName, e.getMessage());
            }
        }
        
        return SheetResult.builder()
                .sheetName(sheetName)
                .storeName(store.getName())
                .rowsProcessed(rowsProcessed)
                .successCount(successCount)
                .errorCount(errorCount)
                .build();
    }
    
    private void processRow(Row row, Store store, String sheetName, List<UploadError> errors) {
        // Extract values
        String name = getStringCellValue(row.getCell(COL_NAME));
        String nameAr = getStringCellValue(row.getCell(COL_NAME_AR));
        String categoryName = getStringCellValue(row.getCell(COL_CATEGORY));
        Double originalPrice = getNumericCellValue(row.getCell(COL_ORIGINAL_PRICE));
        Double discountPrice = getNumericCellValue(row.getCell(COL_DISCOUNT_PRICE));
        String description = getStringCellValue(row.getCell(COL_DESCRIPTION));
        String descriptionAr = getStringCellValue(row.getCell(COL_DESCRIPTION_AR));
        String image1 = getStringCellValue(row.getCell(COL_IMAGE1));
        String image2 = getStringCellValue(row.getCell(COL_IMAGE2));
        String image3 = getStringCellValue(row.getCell(COL_IMAGE3));
        
        // Validate required fields
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Item name is required");
        }
        if (categoryName == null || categoryName.isBlank()) {
            throw new IllegalArgumentException("Category is required");
        }
        
        // Lookup or create category by path (supports "Parent.Child.Grandchild" format)
        Category category = findOrCreateCategoryByPath(categoryName);
        
        // Build images list
        List<String> images = new ArrayList<>();
        if (image1 != null && !image1.isBlank()) images.add(image1);
        if (image2 != null && !image2.isBlank()) images.add(image2);
        if (image3 != null && !image3.isBlank()) images.add(image3);
        
        // Find or create ReferenceItem and link it to the store
        ReferenceItem referenceItem = findOrCreateReferenceItem(
                name, nameAr, category, description, descriptionAr, images, store
        );
        
        // Create or update StoreItem with prices
        createOrUpdateStoreItem(referenceItem, store, originalPrice, discountPrice);
    }
    
    private ReferenceItem findOrCreateReferenceItem(
            String name, String nameAr, Category category,
            String description, String descriptionAr, List<String> images, Store store
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
            // Add store to specificStoreIds if not already present
            List<String> storeIds = item.getSpecificStoreIds();
            if (storeIds == null) {
                storeIds = new ArrayList<>();
            }
            if (!storeIds.contains(store.getId())) {
                storeIds.add(store.getId());
                item.setSpecificStoreIds(storeIds);
            }
            return referenceItemRepository.save(item);
        }
        
        // Create new reference item with store in specificStoreIds
        List<String> storeIds = new ArrayList<>();
        storeIds.add(store.getId());
        
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
    
    private void createOrUpdateStoreItem(ReferenceItem referenceItem, Store store,
                                         Double originalPrice, Double discountPrice) {
        // Check if StoreItem already exists for this store-item combination
        Optional<StoreItem> existingStoreItem = storeItemRepository
                .findByStoreIdAndReferenceItemId(store.getId(), referenceItem.getId());
        
        if (existingStoreItem.isPresent()) {
            // Update existing store item prices
            StoreItem storeItem = existingStoreItem.get();
            if (originalPrice != null) {
                storeItem.setOriginalPrice(originalPrice);
            }
            if (discountPrice != null) {
                storeItem.setDiscountPrice(discountPrice);
            }
            storeItem.setLastPriceUpdate(Instant.now());
            storeItemRepository.save(storeItem);
        } else {
            // Create new store item
            StoreItem newStoreItem = StoreItem.builder()
                    .storeId(store.getId())
                    .referenceItemId(referenceItem.getId())
                    .name(referenceItem.getName())
                    .nameAr(referenceItem.getNameAr())
                    .images(referenceItem.getImages())
                    .originalPrice(originalPrice)
                    .discountPrice(discountPrice)
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
    
    /**
     * Find or create a category by path.
     * Path format: "Parent.Child.Grandchild" where each segment is a category name.
     * Creates missing categories as subcategories of the previous segment.
     * 
     * @param path Category path (e.g., "Dairy.Milk.Low Fat")
     * @return The final (deepest) category in the path
     */
    private Category findOrCreateCategoryByPath(String path) {
        String[] segments = path.split("\\.");
        
        Category currentCategory = null;
        
        for (int i = 0; i < segments.length; i++) {
            String segmentName = segments[i].trim();
            if (segmentName.isBlank()) {
                throw new IllegalArgumentException("Empty category segment in path: " + path);
            }
            
            Optional<Category> foundCategory;
            
            if (i == 0) {
                // First segment: look for top-level category (null parent) or any category
                foundCategory = findTopLevelCategoryByName(segmentName);
                if (foundCategory.isEmpty()) {
                    // Fallback: try to find any category with this name
                    foundCategory = findCategoryByName(segmentName);
                }
            } else {
                // Subsequent segments: look for child of current category
                foundCategory = findChildCategoryByName(currentCategory.getId(), segmentName);
            }
            
            if (foundCategory.isPresent()) {
                currentCategory = foundCategory.get();
            } else {
                // Create the category
                currentCategory = createCategory(segmentName, currentCategory);
            }
        }
        
        return currentCategory;
    }
    
    private Optional<Category> findTopLevelCategoryByName(String name) {
        // Try English name first
        Optional<Category> category = categoryRepository.findByParentCategoryIdIsNullAndNameIgnoreCase(name);
        if (category.isPresent()) {
            return category;
        }
        // Try Arabic name
        return categoryRepository.findByParentCategoryIdIsNullAndNameArIgnoreCase(name);
    }
    
    private Optional<Category> findChildCategoryByName(String parentId, String name) {
        // Try English name first
        Optional<Category> category = categoryRepository.findByParentCategoryIdAndNameIgnoreCase(parentId, name);
        if (category.isPresent()) {
            return category;
        }
        // Try Arabic name
        return categoryRepository.findByParentCategoryIdAndNameArIgnoreCase(parentId, name);
    }
    
    private Optional<Category> findCategoryByName(String name) {
        // Try English name first
        Optional<Category> category = categoryRepository.findByNameIgnoreCase(name);
        if (category.isPresent()) {
            return category;
        }
        // Try Arabic name
        return categoryRepository.findByNameArIgnoreCase(name);
    }
    
    private Category createCategory(String name, Category parent) {
        Category newCategory = Category.builder()
                .name(name)
                .parentCategoryId(parent != null ? parent.getId() : null)
                .active(true)
                .displayOrder(0)
                .build();
        
        Category saved = categoryRepository.save(newCategory);
        
        // Update parent's subcategoryIds if parent exists
        if (parent != null) {
            List<String> subcategoryIds = parent.getSubcategoryIds();
            if (subcategoryIds == null) {
                subcategoryIds = new ArrayList<>();
            }
            subcategoryIds.add(saved.getId());
            parent.setSubcategoryIds(subcategoryIds);
            categoryRepository.save(parent);
        }
        
        log.info("Created category '{}' under parent '{}'", name, parent != null ? parent.getName() : "(root)");
        return saved;
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
