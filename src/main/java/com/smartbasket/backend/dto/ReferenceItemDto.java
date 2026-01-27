package com.smartbasket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferenceItemDto {
    private String id;
    private String name;
    private String nameAr;
    private String categoryId;
    private String category; // Denormalized category name
    private String categoryBreadcrumb; // Full path e.g., "Dairy > Milk"
    private String description;
    private String descriptionAr;
    private List<String> images;
    private String barcode;
    private boolean availableInAllStores;
    private List<String> specificStoreIds;
    private boolean active;
}

