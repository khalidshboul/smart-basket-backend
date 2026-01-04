package com.smartbasket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreItemDto {
    private String id;
    private String storeId;
    private String storeName;
    private String referenceItemId;
    private String referenceItemName;
    private String name;
    private String nameAr;
    private String brand;
    private String barcode;
    private List<String> images;
    
    // Current price info
    private Double discountPrice;
    private Double originalPrice;
    private Double discountPercentage; // Calculated field
    private String currency;
    private Boolean isPromotion;
    private Instant lastPriceUpdate;
}
