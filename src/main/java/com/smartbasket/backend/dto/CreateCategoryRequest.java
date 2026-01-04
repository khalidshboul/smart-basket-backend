package com.smartbasket.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;
    private String nameAr;
    
    private String icon;
    private String description;
    private String descriptionAr;
    private Integer displayOrder;
    private Boolean active;
}


