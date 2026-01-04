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
public class BatchPriceUpdateResponse {
    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<PriceUpdateResult> results;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceUpdateResult {
        private String storeItemId;
        private boolean success;
        private String message;
        private Double newPrice;
    }
}
