package com.smartbasket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}


