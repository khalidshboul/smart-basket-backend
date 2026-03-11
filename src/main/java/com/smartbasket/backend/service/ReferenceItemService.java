package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.BarcodeSearchResponse;
import com.smartbasket.backend.dto.CreateReferenceItemRequest;
import com.smartbasket.backend.dto.ReferenceItemDto;
import com.smartbasket.backend.dto.StoreItemDto;
import com.smartbasket.backend.exception.ResourceNotFoundException;
import com.smartbasket.backend.mapper.ReferenceItemMapper;
import com.smartbasket.backend.model.Category;
import com.smartbasket.backend.model.ReferenceItem;
import com.smartbasket.backend.model.Store;
import com.smartbasket.backend.model.StoreItem;
import com.smartbasket.backend.repository.CategoryRepository;
import com.smartbasket.backend.repository.ReferenceItemRepository;
import com.smartbasket.backend.repository.StoreItemRepository;
import com.smartbasket.backend.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReferenceItemService {

    private final ReferenceItemRepository referenceItemRepository;
    private final CategoryRepository categoryRepository;
    private final StoreItemRepository storeItemRepository;
    private final StoreRepository storeRepository;
    private final ReferenceItemMapper referenceItemMapper;

    // ──────────────────────────────────────────────
    // List / Search (batch-aware mapper)
    // ──────────────────────────────────────────────

    /**
     * Paginated items endpoint with support for search and category filtering.
     * Pre-fetches all categories to avoid N+1.
     */
    public Page<ReferenceItemDto> getAllItems(String query, String categoryId, Pageable pageable) {
        Page<ReferenceItem> page;
        
        if (query != null && !query.trim().isEmpty()) {
            page = referenceItemRepository.findByNameContainingIgnoreCase(query.trim(), pageable);
        } else if (categoryId != null && !categoryId.trim().isEmpty()) {
            page = referenceItemRepository.findByCategoryId(categoryId.trim(), pageable);
        } else {
            page = referenceItemRepository.findAll(pageable);
        }
        
        List<ReferenceItemDto> dtos = referenceItemMapper.toDtoList(page.getContent());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    /**
     * Paginated items endpoint. (Kept for backward internal usage if needed)
     */
    public Page<ReferenceItemDto> getAllItems(Pageable pageable) {
        return getAllItems(null, null, pageable);
    }

    /**
     * Returns all items without pagination (for admin pages that need the full list).
     * Uses the batch-aware mapper to avoid N+1.
     */
    public List<ReferenceItemDto> getAllItems() {
        return referenceItemMapper.toDtoList(referenceItemRepository.findAll());
    }

    public List<ReferenceItemDto> getItemsByCategory(String categoryId) {
        List<ReferenceItem> items = referenceItemRepository.findByCategoryId(categoryId);
        return referenceItemMapper.toDtoList(items);
    }

    /**
     * Get items by category ID including items from all subcategories.
     */
    public List<ReferenceItemDto> getItemsByCategoryIncludingSubcategories(String categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }

        List<String> categoryIds = new ArrayList<>();
        categoryIds.add(categoryId);

        List<Category> subcategories = categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(categoryId);
        subcategories.forEach(sub -> categoryIds.add(sub.getId()));

        List<ReferenceItem> items = referenceItemRepository.findByCategoryIdIn(categoryIds);
        return referenceItemMapper.toDtoList(items);
    }

    public List<ReferenceItemDto> searchItems(String query) {
        List<ReferenceItem> items = referenceItemRepository.findByNameContainingIgnoreCase(query);
        return referenceItemMapper.toDtoList(items);
    }

    // ──────────────────────────────────────────────
    // Single-Item CRUD (uses single-entity mapper)
    // ──────────────────────────────────────────────

    public Optional<ReferenceItemDto> getItemById(String id) {
        return referenceItemRepository.findById(id)
                .map(referenceItemMapper::toDto);
    }

    public ReferenceItemDto createItem(CreateReferenceItemRequest request) {
        String categoryName = getCategoryName(request.getCategoryId());

        ReferenceItem entity = referenceItemMapper.toEntity(request);
        entity.setCategory(categoryName);

        ReferenceItem saved = referenceItemRepository.save(entity);
        return referenceItemMapper.toDto(saved);
    }

    @Transactional
    public Optional<ReferenceItemDto> updateItem(String id, CreateReferenceItemRequest request) {
        String categoryName = getCategoryName(request.getCategoryId());

        return referenceItemRepository.findById(id)
                .map(existing -> {
                    // Detect removed stores for cascade deletion
                    List<String> oldSpecificStoreIds = existing.getSpecificStoreIds() != null
                            ? existing.getSpecificStoreIds() : new ArrayList<>();
                    List<String> newSpecificStoreIds = request.getSpecificStoreIds() != null
                            ? request.getSpecificStoreIds() : new ArrayList<>();

                    // Find stores that were removed
                    Set<String> removedStoreIds = new HashSet<>(oldSpecificStoreIds);
                    removedStoreIds.removeAll(newSpecificStoreIds);

                    // Check if switching from "available in all stores" to specific stores
                    boolean wasAvailableInAll = existing.isAvailableInAllStores();
                    boolean willBeAvailableInAll = request.getAvailableInAllStores() != null
                            ? request.getAvailableInAllStores() : wasAvailableInAll;

                    // If switching to restricted mode, delete StoreItems for stores not in new list
                    if (wasAvailableInAll && !willBeAvailableInAll && !newSpecificStoreIds.isEmpty()) {
                        List<String> existingStoreIds = storeItemRepository.findByReferenceItemId(id)
                                .stream()
                                .map(StoreItem::getStoreId)
                                .distinct()
                                .toList();
                        Set<String> storesToRemove = new HashSet<>(existingStoreIds);
                        storesToRemove.removeAll(newSpecificStoreIds);
                        if (!storesToRemove.isEmpty()) {
                            storeItemRepository.deleteByReferenceItemIdAndStoreIdIn(id, new ArrayList<>(storesToRemove));
                        }
                    }

                    // If stores were explicitly removed from specificStoreIds, cascade delete
                    if (!removedStoreIds.isEmpty() && !willBeAvailableInAll) {
                        storeItemRepository.deleteByReferenceItemIdAndStoreIdIn(id, new ArrayList<>(removedStoreIds));
                    }

                    // Apply all field updates
                    existing.setName(request.getName());
                    existing.setNameAr(request.getNameAr());
                    existing.setCategoryId(request.getCategoryId());
                    existing.setCategory(categoryName);
                    existing.setDescription(request.getDescription());
                    existing.setDescriptionAr(request.getDescriptionAr());
                    existing.setImages(request.getImages() != null ? request.getImages() : existing.getImages());
                    existing.setBarcode(request.getBarcode());
                    if (request.getAvailableInAllStores() != null) {
                        existing.setAvailableInAllStores(request.getAvailableInAllStores());
                    }
                    existing.setSpecificStoreIds(request.getSpecificStoreIds() != null ? request.getSpecificStoreIds() : existing.getSpecificStoreIds());

                    return referenceItemRepository.save(existing);
                })
                .map(referenceItemMapper::toDto);
    }

    @Transactional
    public boolean deleteItem(String id) {
        if (referenceItemRepository.existsById(id)) {
            storeItemRepository.deleteAll(storeItemRepository.findByReferenceItemId(id));
            referenceItemRepository.deleteById(id);
            return true;
        }
        return false;
    }

    public Optional<ReferenceItemDto> toggleStatus(String id) {
        return referenceItemRepository.findById(id)
                .map(existing -> {
                    existing.setActive(!existing.isActive());
                    return referenceItemRepository.save(existing);
                })
                .map(referenceItemMapper::toDto);
    }

    // ──────────────────────────────────────────────
    // Barcode Search
    // ──────────────────────────────────────────────

    /**
     * Search for a reference item by barcode and return with all store prices.
     * This is a combined query that eliminates the need for multiple API calls.
     */
    public Optional<BarcodeSearchResponse> searchByBarcode(String barcode) {
        if (barcode == null || barcode.trim().isEmpty()) {
            return Optional.empty();
        }

        return referenceItemRepository.findByBarcode(barcode.trim())
                .map(referenceItem -> {
                    ReferenceItemDto itemDto = referenceItemMapper.toDto(referenceItem);

                    List<StoreItemDto> storePrices = storeItemRepository
                            .findByReferenceItemId(referenceItem.getId())
                            .stream()
                            .map(this::toStoreItemDto)
                            .collect(Collectors.toList());

                    String cheapestStore = null;
                    Double lowestPrice = null;
                    for (StoreItemDto si : storePrices) {
                        Double effectivePrice = si.getDiscountPrice() != null ? si.getDiscountPrice()
                                : si.getOriginalPrice();
                        if (effectivePrice != null && (lowestPrice == null || effectivePrice < lowestPrice)) {
                            lowestPrice = effectivePrice;
                            cheapestStore = si.getStoreName();
                        }
                    }

                    return BarcodeSearchResponse.builder()
                            .item(itemDto)
                            .storePrices(storePrices)
                            .storeCount(storePrices.size())
                            .lowestPrice(lowestPrice)
                            .cheapestStoreName(cheapestStore)
                            .build();
                });
    }

    /**
     * Convert StoreItem entity to DTO with store name.
     */
    private StoreItemDto toStoreItemDto(StoreItem storeItem) {
        String storeName = storeItem.getStoreId() != null
                ? storeRepository.findById(storeItem.getStoreId())
                    .map(Store::getName)
                    .orElse(null)
                : null;

        return StoreItemDto.builder()
                .id(storeItem.getId())
                .storeId(storeItem.getStoreId())
                .storeName(storeName)
                .referenceItemId(storeItem.getReferenceItemId())
                .name(storeItem.getName())
                .nameAr(storeItem.getNameAr())
                .brand(storeItem.getBrand())
                .originalPrice(storeItem.getOriginalPrice())
                .discountPrice(storeItem.getDiscountPrice())
                .currency(storeItem.getCurrency())
                .isPromotion(storeItem.getIsPromotion() != null && storeItem.getIsPromotion())
                .barcode(storeItem.getBarcode())
                .build();
    }

    private String getCategoryName(String categoryId) {
        return categoryRepository.findById(categoryId)
                .map(Category::getName)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}
