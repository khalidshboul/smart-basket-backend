package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.*;
import com.smartbasket.backend.model.Store;
import com.smartbasket.backend.model.StoreItem;
import com.smartbasket.backend.model.StorePrice;
import com.smartbasket.backend.model.ReferenceItem;
import com.smartbasket.backend.repository.StoreItemRepository;
import com.smartbasket.backend.repository.StorePriceRepository;
import com.smartbasket.backend.repository.StoreRepository;
import com.smartbasket.backend.repository.ReferenceItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BasketComparisonService {

    private final ReferenceItemRepository referenceItemRepository;
    private final StoreRepository storeRepository;
    private final StoreItemRepository storeItemRepository;
    private final StorePriceRepository storePriceRepository;

    private static final String DEFAULT_CURRENCY = "JOD";

    public BasketComparisonResponse compareBasket(BasketComparisonRequest request) {
        // 1. Get all ACTIVE reference items in the basket
        List<ReferenceItem> basketReferenceItems = referenceItemRepository
                .findAllById(request.getReferenceItemIds())
                .stream()
                .filter(ReferenceItem::isActive) // Exclude inactive items
                .toList();

        // Build basket item info list
        List<BasketItemInfo> basketItemInfos = basketReferenceItems.stream()
                .map(item -> BasketItemInfo.builder()
                        .referenceItemId(item.getId())
                        .name(item.getName())
                        .category(item.getCategory())
                        .build())
                .collect(Collectors.toList());

        // 2. Get all active stores
        List<Store> activeStores = storeRepository.findByActiveTrue();

        // 3. For each store, calculate the basket total
        List<StoreComparisonResult> storeResults = new ArrayList<>();

        for (Store store : activeStores) {
            StoreComparisonResult result = calculateStoreTotal(store, basketReferenceItems);
            storeResults.add(result);
        }

        // 4. Sort by: 1) Number of missing items (fewer is better), 2) Then by price (cheapest first)
        storeResults.sort((a, b) -> {
            // First: Sort by number of missing items (stores with fewer missing items come first)
            int missingComparison = Integer.compare(a.getMissingItems().size(), b.getMissingItems().size());
            if (missingComparison != 0) {
                return missingComparison;
            }
            // Second: If same number of missing items, sort by price (cheapest first)
            return Double.compare(a.getTotalPrice(), b.getTotalPrice());
        });

        // 5. Calculate savings and find cheapest
        Double lowestTotal = storeResults.stream()
                .filter(StoreComparisonResult::isAllItemsAvailable)
                .mapToDouble(StoreComparisonResult::getTotalPrice)
                .min()
                .orElse(0.0);

        Double highestTotal = storeResults.stream()
                .filter(StoreComparisonResult::isAllItemsAvailable)
                .mapToDouble(StoreComparisonResult::getTotalPrice)
                .max()
                .orElse(0.0);

        StoreComparisonResult cheapest = storeResults.stream()
                .filter(StoreComparisonResult::isAllItemsAvailable)
                .min(Comparator.comparingDouble(StoreComparisonResult::getTotalPrice))
                .orElse(null);

        return BasketComparisonResponse.builder()
                .basketItems(basketItemInfos)
                .storeComparisons(storeResults)
                .cheapestStoreId(cheapest != null ? cheapest.getStoreId() : null)
                .cheapestStoreName(cheapest != null ? cheapest.getStoreName() : null)
                .lowestTotal(lowestTotal)
                .highestTotal(highestTotal)
                .potentialSavings(highestTotal - lowestTotal)
                .build();
    }

    private StoreComparisonResult calculateStoreTotal(Store store, List<ReferenceItem> basketItems) {
        List<StoreItemPriceInfo> itemPrices = new ArrayList<>();
        List<String> missingItems = new ArrayList<>();
        double totalPrice = 0.0;

        for (ReferenceItem refItem : basketItems) {
            // First check if this item is assigned to this store
            // Use specificStoreIds (admin-defined whitelist) when availableInAllStores is false
            boolean isAssignedToStore = refItem.isAvailableInAllStores() || 
                    (refItem.getSpecificStoreIds() != null && refItem.getSpecificStoreIds().contains(store.getId()));
            
            if (!isAssignedToStore) {
                // Item is not assigned to this store - mark as missing
                missingItems.add(refItem.getName());
                itemPrices.add(StoreItemPriceInfo.builder()
                        .referenceItemId(refItem.getId())
                        .referenceItemName(refItem.getName())
                        .available(false)
                        .price(0.0)
                        .currency(DEFAULT_CURRENCY)
                        .build());
                continue;
            }
            
            // Find the store item for this reference item at this store
            List<StoreItem> storeItems = storeItemRepository.findByReferenceItemId(refItem.getId())
                    .stream()
                    .filter(si -> si.getStoreId().equals(store.getId()))
                    .toList();

            if (storeItems.isEmpty()) {
                // Item not available at this store
                missingItems.add(refItem.getName());
                itemPrices.add(StoreItemPriceInfo.builder()
                        .referenceItemId(refItem.getId())
                        .referenceItemName(refItem.getName())
                        .available(false)
                        .price(0.0)
                        .currency(DEFAULT_CURRENCY)
                        .build());
            } else {
                // Use cached price from StoreItem (no need to query StorePrice table)
                StoreItem storeItem = storeItems.get(0);
                
                // Use discount price, fallback to original price if not set
                Double effectivePrice = storeItem.getDiscountPrice();
                if (effectivePrice == null || effectivePrice <= 0) {
                    effectivePrice = storeItem.getOriginalPrice();
                }
                
                if (effectivePrice != null && effectivePrice > 0) {
                    totalPrice += effectivePrice;

                    itemPrices.add(StoreItemPriceInfo.builder()
                            .referenceItemId(refItem.getId())
                            .referenceItemName(refItem.getName())
                            .storeItemId(storeItem.getId())
                            .storeItemName(storeItem.getName())
                            .brand(storeItem.getBrand())
                            .price(effectivePrice)
                            .currency(storeItem.getCurrency() != null ? storeItem.getCurrency() : DEFAULT_CURRENCY)
                            .isPromotion(storeItem.getIsPromotion() != null && storeItem.getIsPromotion())
                            .available(true)
                            .build());
                } else {
                    // No price data available
                    missingItems.add(refItem.getName());
                    itemPrices.add(StoreItemPriceInfo.builder()
                            .referenceItemId(refItem.getId())
                            .referenceItemName(refItem.getName())
                            .storeItemId(storeItem.getId())
                            .storeItemName(storeItem.getName())
                            .brand(storeItem.getBrand())
                            .available(false)
                            .price(0.0)
                            .currency(DEFAULT_CURRENCY)
                            .build());
                }
            }
        }

        return StoreComparisonResult.builder()
                .storeId(store.getId())
                .storeName(store.getName())
                .storeLogoUrl(store.getLogoUrl())
                .totalPrice(totalPrice)
                .currency(DEFAULT_CURRENCY)
                .allItemsAvailable(missingItems.isEmpty())
                .itemPrices(itemPrices)
                .missingItems(missingItems)
                .availableItemCount(basketItems.size() - missingItems.size())
                .totalItemCount(basketItems.size())
                .build();
    }
}
