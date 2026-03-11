package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.BulkUploadParsedData;
import com.smartbasket.backend.dto.BulkUploadParsedData.ParsedRow;
import com.smartbasket.backend.dto.BulkUploadResponseDto;
import com.smartbasket.backend.exception.ResourceNotFoundException;
import com.smartbasket.backend.model.Category;
import com.smartbasket.backend.model.ReferenceItem;
import com.smartbasket.backend.model.StoreItem;
import com.smartbasket.backend.repository.CategoryRepository;
import com.smartbasket.backend.repository.ReferenceItemRepository;
import com.smartbasket.backend.repository.StoreItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Orchestrates the bulk upload workflow: parse → persist.
 * <p>
 * Excel parsing is delegated to {@link BulkUploadExcelParser}.
 * Persistence uses batch operations for optimal performance.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BulkUploadService {

    private final CategoryRepository categoryRepository;
    private final ReferenceItemRepository referenceItemRepository;
    private final StoreItemRepository storeItemRepository;
    private final MongoTemplate mongoTemplate;
    private final BulkUploadExcelParser excelParser;

    /**
     * Processes a bulk upload Excel file for the given category.
     *
     * @param file       the uploaded .xlsx file
     * @param categoryId the target category ID
     * @return summary of the upload result
     */
    @Transactional(rollbackFor = Exception.class)
    public BulkUploadResponseDto processExcelFile(MultipartFile file, String categoryId) throws IOException {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));

        BulkUploadParsedData parsedData = excelParser.parse(file, categoryId, category.getName());

        int successCount = persistBatch(parsedData.getRows(), category);

        log.info("Bulk upload completed: {} items processed for category '{}'", successCount, category.getName());

        return BulkUploadResponseDto.builder()
                .success(true)
                .totalRows(parsedData.getTotalRows())
                .successCount(successCount)
                .errorCount(0)
                .categoryId(categoryId)
                .categoryName(category.getName())
                .build();
    }

    // ──────────────────────────────────────────────
    // Batch Persistence
    // ──────────────────────────────────────────────

    /**
     * Persists all parsed rows using batch operations:
     * <ol>
     *   <li>Pre-fetch existing {@link ReferenceItem}s by name (single query)</li>
     *   <li>Batch-save all {@link ReferenceItem}s (single {@code saveAll} call)</li>
     *   <li>Bulk-upsert all {@link StoreItem}s via {@link MongoTemplate} (single write)</li>
     * </ol>
     */
    private int persistBatch(List<ParsedRow> rows, Category category) {
        Map<String, ReferenceItem> existingItemsByName = prefetchReferenceItems(rows);

        List<ReferenceItem> itemsToSave = buildReferenceItems(rows, category, existingItemsByName);
        List<ReferenceItem> savedItems = referenceItemRepository.saveAll(itemsToSave);

        Map<String, ReferenceItem> savedItemsByName = indexByLowerCaseName(savedItems);
        bulkUpsertStoreItems(rows, savedItemsByName);

        return rows.size();
    }

    // ──────────────────────────────────────────────
    // ReferenceItem Handling
    // ──────────────────────────────────────────────

    private Map<String, ReferenceItem> prefetchReferenceItems(List<ParsedRow> rows) {
        List<String> names = rows.stream().map(ParsedRow::name).toList();
        return referenceItemRepository.findByNameIn(names).stream()
                .collect(Collectors.toMap(
                        item -> item.getName().toLowerCase(),
                        item -> item,
                        (existing, duplicate) -> existing
                ));
    }

    private List<ReferenceItem> buildReferenceItems(
            List<ParsedRow> rows, Category category,
            Map<String, ReferenceItem> existingItemsByName) {

        List<ReferenceItem> itemsToSave = new ArrayList<>();

        for (ParsedRow row : rows) {
            String key = row.name().toLowerCase();
            ReferenceItem item = existingItemsByName.get(key);

            if (item != null) {
                mergeFields(item, row, category);
                mergeStoreIds(item, row.storePrices().keySet());
            } else {
                item = ReferenceItem.builder()
                        .name(row.name())
                        .nameAr(row.nameAr())
                        .categoryId(category.getId())
                        .category(category.getName())
                        .description(row.description())
                        .descriptionAr(row.descriptionAr())
                        .images(row.images())
                        .active(true)
                        .availableInAllStores(false)
                        .specificStoreIds(new ArrayList<>(row.storePrices().keySet()))
                        .build();
                existingItemsByName.put(key, item);
            }

            itemsToSave.add(item);
        }
        return itemsToSave;
    }

    /**
     * Updates an existing item: re-categorizes it to the upload's category
     * and fills in any blank fields from the parsed row.
     */
    private void mergeFields(ReferenceItem item, ParsedRow row, Category category) {
        item.setCategoryId(category.getId());
        item.setCategory(category.getName());
        setIfBlank(item.getNameAr(), row.nameAr(), item::setNameAr);
        setIfBlank(item.getDescription(), row.description(), item::setDescription);
        setIfBlank(item.getDescriptionAr(), row.descriptionAr(), item::setDescriptionAr);
        if ((item.getImages() == null || item.getImages().isEmpty()) && !row.images().isEmpty()) {
            item.setImages(row.images());
        }
    }

    private void mergeStoreIds(ReferenceItem item, Set<String> newStoreIds) {
        List<String> current = item.getSpecificStoreIds();
        if (current == null) {
            current = new ArrayList<>();
        }
        for (String storeId : newStoreIds) {
            if (!current.contains(storeId)) {
                current.add(storeId);
            }
        }
        item.setSpecificStoreIds(current);
    }

    // ──────────────────────────────────────────────
    // StoreItem Bulk Upsert
    // ──────────────────────────────────────────────

    private void bulkUpsertStoreItems(List<ParsedRow> rows, Map<String, ReferenceItem> savedItemsByName) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, StoreItem.class);
        int upsertCount = 0;

        for (ParsedRow row : rows) {
            ReferenceItem savedItem = savedItemsByName.get(row.name().toLowerCase());
            if (savedItem == null) {
                continue;
            }

            for (Map.Entry<String, Double> priceEntry : row.storePrices().entrySet()) {
                bulkOps.upsert(
                        storeItemMatchQuery(priceEntry.getKey(), savedItem.getId()),
                        storeItemUpdate(priceEntry.getKey(), priceEntry.getValue(), savedItem)
                );
                upsertCount++;
            }
        }

        if (upsertCount > 0) {
            bulkOps.execute();
        }
    }

    private Query storeItemMatchQuery(String storeId, String referenceItemId) {
        return new Query(Criteria.where("storeId").is(storeId)
                .and("referenceItemId").is(referenceItemId));
    }

    private Update storeItemUpdate(String storeId, Double originalPrice, ReferenceItem item) {
        return new Update()
                .set("originalPrice", originalPrice)
                .set("lastPriceUpdate", Instant.now())
                .setOnInsert("storeId", storeId)
                .setOnInsert("referenceItemId", item.getId())
                .setOnInsert("name", item.getName())
                .setOnInsert("nameAr", item.getNameAr())
                .setOnInsert("images", item.getImages())
                .setOnInsert("currency", "JOD");
    }

    // ──────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────

    private Map<String, ReferenceItem> indexByLowerCaseName(List<ReferenceItem> items) {
        return items.stream()
                .collect(Collectors.toMap(
                        item -> item.getName().toLowerCase(),
                        item -> item,
                        (existing, duplicate) -> duplicate
                ));
    }

    /**
     * Sets a value via the setter only if the current value is blank/null and the new value is non-null.
     */
    private void setIfBlank(String currentValue, String newValue, java.util.function.Consumer<String> setter) {
        if ((currentValue == null || currentValue.isBlank()) && newValue != null) {
            setter.accept(newValue);
        }
    }
}
