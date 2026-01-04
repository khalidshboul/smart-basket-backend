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
public class BasketComparisonResponse {
    private List<BasketItemInfo> basketItems;
    private List<StoreComparisonResult> storeComparisons;
    private String cheapestStoreId;
    private String cheapestStoreName;
    private Double lowestTotal;
    private Double highestTotal;
    private Double potentialSavings;
}
