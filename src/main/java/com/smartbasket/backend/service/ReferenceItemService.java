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

    public List<ReferenceItemDto> getAllItems() {
        return referenceItemRepository.findAll()
                .stream()
                .map(referenceItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<ReferenceItemDto> getItemById(String id) {
        return referenceItemRepository.findById(id)
                .map(referenceItemMapper::toDto);
    }

    public List<ReferenceItemDto> getItemsByCategory(String categoryId) {
        return referenceItemRepository.findByCategoryId(categoryId)
                .stream()
                .map(referenceItemMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get items by category ID including items from all subcategories
     */
    public List<ReferenceItemDto> getItemsByCategoryIncludingSubcategories(String categoryId) {
        // Validate category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found: " + categoryId);
        }
        
        // Collect category IDs (parent + all subcategories)
        List<String> categoryIds = new ArrayList<>();
        categoryIds.add(categoryId);
        
        // Get subcategory IDs
        List<Category> subcategories = categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(categoryId);
        subcategories.forEach(sub -> categoryIds.add(sub.getId()));
        
        // Query items in all these categories
        return referenceItemRepository.findByCategoryIdIn(categoryIds)
                .stream()
                .map(referenceItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<ReferenceItemDto> searchItems(String query) {
        return referenceItemRepository.findByNameContainingIgnoreCase(query)
                .stream()
                .map(referenceItemMapper::toDto)
                .collect(Collectors.toList());
    }

    public ReferenceItemDto createItem(CreateReferenceItemRequest request) {
        // Validate and get category
        String categoryName = getCategoryName(request.getCategoryId());
        
        ReferenceItem entity = referenceItemMapper.toEntity(request);
        entity.setCategory(categoryName); // Set denormalized category name
        
        ReferenceItem saved = referenceItemRepository.save(entity);
        return referenceItemMapper.toDto(saved);
    }

    @Transactional
    public Optional<ReferenceItemDto> updateItem(String id, CreateReferenceItemRequest request) {
        // Validate and get category
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
                        // Find all existing StoreItems for this reference and get their storeIds
                        List<String> existingStoreIds = storeItemRepository.findByReferenceItemId(id)
                                .stream()
                                .map(si -> si.getStoreId())
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
            // Also delete all StoreItems linked to this reference item
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
                    
                    // Get all store items for this reference item
                    List<StoreItemDto> storePrices = storeItemRepository
                            .findByReferenceItemId(referenceItem.getId())
                            .stream()
                            .map(this::toStoreItemDto)
                            .collect(Collectors.toList());
                    
                    // Find cheapest store
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
     * Convert StoreItem entity to DTO with store name
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
