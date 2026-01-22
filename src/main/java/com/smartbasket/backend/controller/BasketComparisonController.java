package com.smartbasket.backend.controller;

import com.smartbasket.backend.dto.BasketComparisonRequest;
import com.smartbasket.backend.dto.BasketComparisonResponse;
import com.smartbasket.backend.service.BasketComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket")
@RequiredArgsConstructor
public class BasketComparisonController {

    private final BasketComparisonService basketComparisonService;

    @PostMapping("/compare")
    public ResponseEntity<BasketComparisonResponse> compareBasket(
            @Valid @RequestBody BasketComparisonRequest request) {
        BasketComparisonResponse response = basketComparisonService.compareBasket(request);
        return ResponseEntity.ok(response);
    }
}
