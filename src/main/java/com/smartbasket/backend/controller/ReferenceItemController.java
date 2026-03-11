package com.smartbasket.backend.controller;

import com.smartbasket.backend.dto.BarcodeSearchResponse;
import com.smartbasket.backend.dto.CreateReferenceItemRequest;
import com.smartbasket.backend.dto.ReferenceItemDto;
import com.smartbasket.backend.service.ReferenceItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ReferenceItemController {

    private final ReferenceItemService referenceItemService;

    /**
     * Returns items, optionally paginated.
     * - With page/size params → returns Page<ReferenceItemDto> (paginated)
     * - Without page/size → returns List<ReferenceItemDto> (all items, backward compatible)
     */
    @GetMapping
    public ResponseEntity<?> getAllItems(
            @RequestParam(name = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(name = "size", required = false, defaultValue = "25") Integer size,
            @RequestParam(name = "paginate", defaultValue = "false") boolean paginate) {

        log.info("Get All Items - page: {}, size: {}, paginate: {}", page, size, paginate);

        // If explicitly asked for pagination OR if page/size params are present
        if (paginate || (page != null && size != null)) {
            int pageVal = (page != null) ? page : 0;
            int sizeVal = (size != null) ? size : 20;
            Pageable pageable = PageRequest.of(pageVal, sizeVal);
            Page<ReferenceItemDto> result = referenceItemService.getAllItems(pageable);
            return ResponseEntity.ok(result);
        }

        // Return full list only if paginate=false is explicitly sent
        return ResponseEntity.ok(referenceItemService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReferenceItemDto> getItemById(@PathVariable String id) {
        return referenceItemService.getItemById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<ReferenceItemDto>> getItemsByCategory(
            @PathVariable String categoryId,
            @RequestParam(defaultValue = "false") boolean includeSubcategories) {
        if (includeSubcategories) {
            return ResponseEntity.ok(referenceItemService.getItemsByCategoryIncludingSubcategories(categoryId));
        }
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

    /**
     * Search for a reference item by barcode.
     * Returns the reference item along with all store prices in a single response.
     */
    @GetMapping("/barcode/{barcode}")
    public ResponseEntity<BarcodeSearchResponse> getByBarcode(@PathVariable String barcode) {
        return referenceItemService.searchByBarcode(barcode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
