package com.smartbasket.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BasketComparisonRequest {
    @NotEmpty(message = "Basket must contain at least one item")
    private List<String> referenceItemIds;
}
