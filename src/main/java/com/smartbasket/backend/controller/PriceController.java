package com.smartbasket.backend.controller;

import com.smartbasket.backend.dto.BatchPriceUpdateRequest;
import com.smartbasket.backend.dto.BatchPriceUpdateResponse;
import com.smartbasket.backend.model.StorePrice;
import com.smartbasket.backend.service.PriceUpdateService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/prices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PriceController {

    private final PriceUpdateService priceUpdateService;

    /**
     * Update a single item's price
     */
    @PostMapping
    public ResponseEntity<StorePrice> updatePrice(
            @RequestParam @NotBlank String storeItemId,
            @RequestParam @NotNull @Min(0) Double price,
            @RequestParam(required = false) @Min(0) Double originalPrice,
            @RequestParam(required = false) String currency,
            @RequestParam(required = false) Boolean isPromotion) {
        
        StorePrice updatedPrice = priceUpdateService.updatePrice(storeItemId, price, originalPrice, currency, isPromotion);
        return ResponseEntity.status(HttpStatus.CREATED).body(updatedPrice);
    }

    /**
     * Batch update prices for multiple items
     */
    @PostMapping("/batch")
    public ResponseEntity<BatchPriceUpdateResponse> batchUpdatePrices(
            @Valid @RequestBody BatchPriceUpdateRequest request) {
        
        BatchPriceUpdateResponse response = priceUpdateService.batchUpdatePrices(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Get price history for a store item
     */
    @GetMapping("/history/{storeItemId}")
    public ResponseEntity<List<StorePrice>> getPriceHistory(@PathVariable String storeItemId) {
        List<StorePrice> history = priceUpdateService.getPriceHistory(storeItemId);
        return ResponseEntity.ok(history);
    }
}
