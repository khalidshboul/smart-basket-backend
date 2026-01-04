package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.CreateStoreItemRequest;
import com.smartbasket.backend.dto.StoreItemDto;
import com.smartbasket.backend.exception.ResourceNotFoundException;
import com.smartbasket.backend.model.Store;
import com.smartbasket.backend.model.StoreItem;
import com.smartbasket.backend.model.ReferenceItem;
import com.smartbasket.backend.repository.StoreItemRepository;
import com.smartbasket.backend.repository.StoreRepository;
import com.smartbasket.backend.repository.ReferenceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreItemService {

    private final StoreItemRepository storeItemRepository;
    private final ReferenceItemRepository referenceItemRepository;
    private final StoreRepository storeRepository;

    private static final String DEFAULT_CURRENCY = "JOD";

    /**
     * Get all store items
     */
    public List<StoreItemDto> getAll() {
        List<StoreItem> items = storeItemRepository.findAll();
        
        // Build lookup maps for names
        Map<String, String> storeNames = storeRepository.findAll()
                .stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
        Map<String, String> refItemNames = referenceItemRepository.findAll()
                .stream()
                .collect(Collectors.toMap(ReferenceItem::getId, ReferenceItem::getName));
        
        return items.stream()
                .map(item -> toDto(item, 
                        storeNames.getOrDefault(item.getStoreId(), "Unknown"),
                        refItemNames.getOrDefault(item.getReferenceItemId(), "Unknown")))
                .collect(Collectors.toList());
    }

    /**
     * Create a store item and auto-link the store to the reference item
     */
    @Transactional
    public StoreItemDto createStoreItem(CreateStoreItemRequest request) {
        // Validate store exists
        Store store = storeRepository.findById(request.getStoreId())
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + request.getStoreId()));

        // Validate reference item exists
        ReferenceItem refItem = referenceItemRepository.findById(request.getReferenceItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Reference item not found: " + request.getReferenceItemId()));

        // Create the store item
        StoreItem storeItem = StoreItem.builder()
                .storeId(request.getStoreId())
                .referenceItemId(request.getReferenceItemId())
                .name(request.getName())
                .nameAr(request.getNameAr())
                .brand(request.getBrand())
                .barcode(request.getBarcode())
                .images(request.getImages() != null ? request.getImages() : new ArrayList<>())
                .build();

        // Set initial price if provided
        if (request.getInitialPrice() != null) {
            storeItem.setDiscountPrice(request.getInitialPrice());
            storeItem.setOriginalPrice(request.getOriginalPrice());
            storeItem.setCurrency(request.getCurrency() != null ? request.getCurrency() : DEFAULT_CURRENCY);
            storeItem.setIsPromotion(request.getIsPromotion() != null ? request.getIsPromotion() : false);
            storeItem.setLastPriceUpdate(Instant.now());
        }

        StoreItem saved = storeItemRepository.save(storeItem);

        return toDto(saved, store.getName(), refItem.getName());
    }

    /**
     * Get all store items for a reference item
     */
    public List<StoreItemDto> getByReferenceItemId(String referenceItemId) {
        List<StoreItem> items = storeItemRepository.findByReferenceItemId(referenceItemId);
        return enrichWithNames(items);
    }

    /**
     * Get all store items for a specific store
     */
    public List<StoreItemDto> getByStoreId(String storeId) {
        List<StoreItem> items = storeItemRepository.findByStoreId(storeId);
        return enrichWithNames(items);
    }

    /**
     * Get a store item by ID
     */
    public Optional<StoreItemDto> getById(String id) {
        return storeItemRepository.findById(id)
                .map(item -> {
                    String storeName = storeRepository.findById(item.getStoreId())
                            .map(Store::getName).orElse(null);
                    String refItemName = referenceItemRepository.findById(item.getReferenceItemId())
                            .map(ReferenceItem::getName).orElse(null);
                    return toDto(item, storeName, refItemName);
                });
    }

    /**
     * Delete a store item
     */
    @Transactional
    public boolean deleteStoreItem(String id) {
        Optional<StoreItem> optItem = storeItemRepository.findById(id);
        if (optItem.isEmpty()) {
            return false;
        }

        StoreItem item = optItem.get();
        storeItemRepository.delete(item);
        return true;
    }

    /**
     * Enrich store items with store and reference item names
     */
    private List<StoreItemDto> enrichWithNames(List<StoreItem> items) {
        if (items.isEmpty()) {
            return List.of();
        }

        // Batch fetch store and reference item names
        List<String> storeIds = items.stream().map(StoreItem::getStoreId).distinct().toList();
        List<String> refItemIds = items.stream().map(StoreItem::getReferenceItemId).distinct().toList();

        Map<String, String> storeNames = storeRepository.findAllById(storeIds).stream()
                .collect(Collectors.toMap(Store::getId, Store::getName));
        Map<String, String> refItemNames = referenceItemRepository.findAllById(refItemIds).stream()
                .collect(Collectors.toMap(ReferenceItem::getId, ReferenceItem::getName));

        return items.stream()
                .map(item -> toDto(item,
                        storeNames.get(item.getStoreId()),
                        refItemNames.get(item.getReferenceItemId())))
                .collect(Collectors.toList());
    }

    private StoreItemDto toDto(StoreItem item, String storeName, String referenceItemName) {
        Double discountPercentage = null;
        if (item.getOriginalPrice() != null && item.getDiscountPrice() != null && item.getOriginalPrice() > 0 && item.getDiscountPrice() > 0) {
            discountPercentage = ((item.getOriginalPrice() - item.getDiscountPrice()) / item.getOriginalPrice()) * 100;
        }
        
        return StoreItemDto.builder()
                .id(item.getId())
                .storeId(item.getStoreId())
                .storeName(storeName)
                .referenceItemId(item.getReferenceItemId())
                .referenceItemName(referenceItemName)
                .name(item.getName())
                .nameAr(item.getNameAr())
                .brand(item.getBrand())
                .barcode(item.getBarcode())
                .images(item.getImages() != null ? item.getImages() : new ArrayList<>())
                .discountPrice(item.getDiscountPrice())
                .originalPrice(item.getOriginalPrice())
                .discountPercentage(discountPercentage)
                .currency(item.getCurrency())
                .isPromotion(item.getIsPromotion())
                .lastPriceUpdate(item.getLastPriceUpdate())
                .build();
    }
}
