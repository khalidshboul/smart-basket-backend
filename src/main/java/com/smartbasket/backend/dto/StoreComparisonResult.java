package com.smartbasket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreComparisonResult {
    private String storeId;
    private String storeName;
    private String storeLogoUrl;
    private Double totalPrice;
    private String currency;
    private boolean allItemsAvailable;
    private List<StoreItemPriceInfo> itemPrices;
    private List<String> missingItems;
    private int availableItemCount;
    private int totalItemCount;
}
