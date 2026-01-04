package com.smartbasket.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "categories")
public class Category {
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String name;
    private String nameAr;  // Arabic name
    
    private String icon;  // Optional emoji or icon URL
    private String description;
    private String descriptionAr;  // Arabic description
    
    @Builder.Default
    private int displayOrder = 0;
    
    @Builder.Default
    private boolean active = false;
}



