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
public class CreateReferenceItemRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String nameAr;

    @NotBlank(message = "Category ID is required")
    private String categoryId;

    private String description;
    private String descriptionAr;
    private List<String> images;
    
    // Store availability (defaults to true if not specified)
    private Boolean availableInAllStores;
    private List<String> specificStoreIds;
}

