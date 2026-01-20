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
public class CategoryDto {
    private String id;
    private String name;
    private String nameAr;
    private String icon;
    private String description;
    private String descriptionAr;
    private int displayOrder;
    private boolean active;
    
    // Hierarchy fields
    private String parentCategoryId;
    private String parentCategoryName;
    private List<CategoryDto> subcategories;
    private int subcategoryCount;
    private List<String> subcategoryIds; // NEW: Direct list of child IDs
}


