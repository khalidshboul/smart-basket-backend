package com.smartbasket.backend.controller;

import com.smartbasket.backend.dto.CreateStoreItemRequest;
import com.smartbasket.backend.dto.StoreItemDto;
import com.smartbasket.backend.service.StoreItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store-items")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StoreItemController {

    private final StoreItemService storeItemService;

    /**
     * Get all store items
     */
    @GetMapping
    public ResponseEntity<List<StoreItemDto>> getAllStoreItems() {
        List<StoreItemDto> items = storeItemService.getAll();
        return ResponseEntity.ok(items);
    }

    /**
     * Create a store item and auto-link to reference item
     */
    @PostMapping
    public ResponseEntity<StoreItemDto> createStoreItem(@Valid @RequestBody CreateStoreItemRequest request) {
        StoreItemDto created = storeItemService.createStoreItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a store item by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<StoreItemDto> getStoreItem(@PathVariable String id) {
        return storeItemService.getById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all store items for a reference item
     */
    @GetMapping("/by-reference/{referenceItemId}")
    public ResponseEntity<List<StoreItemDto>> getByReferenceItem(@PathVariable String referenceItemId) {
        List<StoreItemDto> items = storeItemService.getByReferenceItemId(referenceItemId);
        return ResponseEntity.ok(items);
    }

    /**
     * Get all store items for a specific store
     */
    @GetMapping("/by-store/{storeId}")
    public ResponseEntity<List<StoreItemDto>> getByStore(@PathVariable String storeId) {
        List<StoreItemDto> items = storeItemService.getByStoreId(storeId);
        return ResponseEntity.ok(items);
    }

    /**
     * Delete a store item and cleanup linkage
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStoreItem(@PathVariable String id) {
        boolean deleted = storeItemService.deleteStoreItem(id);
        if (deleted) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }
}
