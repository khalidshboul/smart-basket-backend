package com.smartbasket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Combined response for barcode search.
 * Returns the reference item along with all store prices in a single response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BarcodeSearchResponse {
    
    // Reference item details
    private ReferenceItemDto item;
    
    // Store items with prices (one per store)
    private List<StoreItemDto> storePrices;
    
    // Summary fields for quick access
    private int storeCount;
    private Double lowestPrice;
    private String cheapestStoreName;
}
