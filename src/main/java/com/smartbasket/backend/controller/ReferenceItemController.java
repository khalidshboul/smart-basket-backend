package com.smartbasket.backend.controller;

import com.smartbasket.backend.dto.CreateReferenceItemRequest;
import com.smartbasket.backend.dto.ReferenceItemDto;
import com.smartbasket.backend.service.ReferenceItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReferenceItemController {

    private final ReferenceItemService referenceItemService;

    @GetMapping
    public ResponseEntity<List<ReferenceItemDto>> getAllItems() {
        return ResponseEntity.ok(referenceItemService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReferenceItemDto> getItemById(@PathVariable String id) {
        return referenceItemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ReferenceItemDto>> getItemsByCategory(@PathVariable String categoryId) {
        return ResponseEntity.ok(referenceItemService.getItemsByCategory(categoryId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ReferenceItemDto>> searchItems(@RequestParam String query) {
        return ResponseEntity.ok(referenceItemService.searchItems(query));
    }

    @PostMapping
    public ResponseEntity<ReferenceItemDto> createItem(@Valid @RequestBody CreateReferenceItemRequest request) {
        ReferenceItemDto created = referenceItemService.createItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ReferenceItemDto> updateItem(
            @PathVariable String id,
            @Valid @RequestBody CreateReferenceItemRequest request) {
        return referenceItemService.updateItem(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable String id) {
        if (referenceItemService.deleteItem(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<ReferenceItemDto> toggleItemStatus(@PathVariable String id) {
        return referenceItemService.toggleStatus(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
