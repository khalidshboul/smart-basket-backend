package com.smartbasket.backend.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "stores")
public class Store {
    @Id
    private String id;
    private String name;
    private String nameAr;  // Arabic name
    private String location;
    private String locationAr;  // Arabic location
    private String logoUrl;
    
    @Builder.Default
    private boolean active = false;
}
