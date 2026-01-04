package com.smartbasket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreItemPriceInfo {
    private String referenceItemId;
    private String referenceItemName;
    private String storeItemId;
    private String storeItemName;
    private String brand;
    private Double price;
    private String currency;
    private boolean isPromotion;
    private boolean available;
}
