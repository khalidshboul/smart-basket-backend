package com.smartbasket.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateStoreItemRequest {
    
    @NotBlank(message = "Store ID is required")
    private String storeId;
    
    @NotBlank(message = "Reference Item ID is required")
    private String referenceItemId;
    
    @NotBlank(message = "Name is required")
    private String name;
    private String nameAr;
    
    private String brand;
    private String barcode;
    private List<String> images;
    
    // Optional: set initial price on creation
    private Double initialPrice;
    private Double originalPrice; // Original price before discount
    private String currency;
    private Boolean isPromotion;
}
