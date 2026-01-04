package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.BatchPriceUpdateRequest;
import com.smartbasket.backend.dto.BatchPriceUpdateResponse;
import com.smartbasket.backend.model.StoreItem;
import com.smartbasket.backend.model.StorePrice;
import com.smartbasket.backend.repository.StoreItemRepository;
import com.smartbasket.backend.repository.StorePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PriceUpdateService {

    private final StoreItemRepository storeItemRepository;
    private final StorePriceRepository storePriceRepository;

    private static final String DEFAULT_CURRENCY = "JOD";

    /**
     * Update a single item's price
     */
    @Transactional
    public StorePrice updatePrice(String storeItemId, Double price, Double originalPrice, String currency, Boolean isPromotion) {
        Optional<StoreItem> optItem = storeItemRepository.findById(storeItemId);
        if (optItem.isEmpty()) {
            throw new IllegalArgumentException("Store item not found: " + storeItemId);
        }

        StoreItem storeItem = optItem.get();
        String effectiveCurrency = currency != null ? currency : DEFAULT_CURRENCY;
        boolean effectivePromotion = isPromotion != null ? isPromotion : false;
        Instant now = Instant.now();

        // 1. Create price history record
        StorePrice priceRecord = StorePrice.builder()
                .storeItemId(storeItemId)
                .price(price)
                .originalPrice(originalPrice)
                .currency(effectiveCurrency)
                .isPromotion(effectivePromotion)
                .timestamp(now)
                .build();
        StorePrice savedPrice = storePriceRepository.save(priceRecord);

        // 2. Update cached price on StoreItem
        storeItem.setDiscountPrice(price);
        storeItem.setOriginalPrice(originalPrice);
        storeItem.setCurrency(effectiveCurrency);
        storeItem.setIsPromotion(effectivePromotion);
        storeItem.setLastPriceUpdate(now);
        storeItemRepository.save(storeItem);

        return savedPrice;
    }

    /**
     * Batch update prices for multiple items
     */
    @Transactional
    public BatchPriceUpdateResponse batchUpdatePrices(BatchPriceUpdateRequest request) {
        List<BatchPriceUpdateResponse.PriceUpdateResult> results = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (BatchPriceUpdateRequest.PriceEntry entry : request.getPrices()) {
            try {
                updatePrice(
                        entry.getStoreItemId(),
                        entry.getPrice(),
                        entry.getOriginalPrice(),
                        entry.getCurrency(),
                        entry.getIsPromotion()
                );
                results.add(BatchPriceUpdateResponse.PriceUpdateResult.builder()
                        .storeItemId(entry.getStoreItemId())
                        .success(true)
                        .message("Price updated successfully")
                        .newPrice(entry.getPrice())
                        .build());
                successCount++;
            } catch (Exception e) {
                results.add(BatchPriceUpdateResponse.PriceUpdateResult.builder()
                        .storeItemId(entry.getStoreItemId())
                        .success(false)
                        .message(e.getMessage())
                        .build());
                failureCount++;
            }
        }

        return BatchPriceUpdateResponse.builder()
                .totalRequested(request.getPrices().size())
                .successCount(successCount)
                .failureCount(failureCount)
                .results(results)
                .build();
    }

    /**
     * Get price history for a store item
     */
    public List<StorePrice> getPriceHistory(String storeItemId) {
        return storePriceRepository.findByStoreItemIdOrderByTimestampDesc(storeItemId);
    }
}
