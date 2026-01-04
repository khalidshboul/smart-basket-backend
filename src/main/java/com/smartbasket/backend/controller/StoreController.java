package com.smartbasket.backend.controller;

import com.smartbasket.backend.dto.CreateStoreRequest;
import com.smartbasket.backend.dto.StoreDto;
import com.smartbasket.backend.exception.ResourceNotFoundException;
import com.smartbasket.backend.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeService;

    @GetMapping
    public ResponseEntity<List<StoreDto>> getAllStores() {
        return ResponseEntity.ok(storeService.getAllStores());
    }

    @GetMapping("/active")
    public ResponseEntity<List<StoreDto>> getActiveStores() {
        return ResponseEntity.ok(storeService.getActiveStores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<StoreDto> getStoreById(@PathVariable String id) {
        return storeService.getStoreById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "id", id));
    }

    @PostMapping
    public ResponseEntity<StoreDto> createStore(@Valid @RequestBody CreateStoreRequest request) {
        StoreDto created = storeService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StoreDto> updateStore(
            @PathVariable String id,
            @Valid @RequestBody CreateStoreRequest request) {
        return storeService.updateStore(id, request)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "id", id));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<StoreDto> toggleStoreStatus(@PathVariable String id) {
        return storeService.toggleStoreStatus(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Store", "id", id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStore(@PathVariable String id) {
        if (storeService.deleteStore(id)) {
            return ResponseEntity.noContent().build();
        }
        throw new ResourceNotFoundException("Store", "id", id);
    }
}
