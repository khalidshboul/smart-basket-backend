package com.smartbasket.backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchPriceUpdateRequest {
    
    @NotEmpty(message = "At least one price entry is required")
    @Valid
    private List<PriceEntry> prices;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceEntry {
        
        @NotBlank(message = "Store Item ID is required")
        private String storeItemId;
        
        @NotNull(message = "Price is required")
        @Min(value = 0, message = "Price must be non-negative")
        private Double price;
        
        @Min(value = 0, message = "Original price must be non-negative")
        private Double originalPrice;
        
        private String currency;
        private Boolean isPromotion;
    }
}
