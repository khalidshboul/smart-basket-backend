package com.smartbasket.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reference_items")
public class ReferenceItem {
    @Id
    private String id;
    private String name;
    private String nameAr;  // Arabic name
    
    // Category reference (new - primary)
    private String categoryId;
    
    // Category name (kept for backward compatibility, denormalized)
    private String category;
    
    private String description;
    private String descriptionAr;  // Arabic description
    
    // Multiple images support (migrated from imageUrl)
    @Builder.Default
    private List<String> images = new ArrayList<>();
    
    // Store availability settings
    @Builder.Default
    private boolean availableInAllStores = true;
    
    // Only used when availableInAllStores is false
    @Builder.Default
    private List<String> specificStoreIds = new ArrayList<>();
    
    // Active status
    @Builder.Default
    private boolean active = false;
}

