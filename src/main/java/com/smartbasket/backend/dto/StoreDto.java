package com.smartbasket.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreDto {
    private String id;
    private String name;
    private String nameAr;
    private String location;
    private String locationAr;
    private String logoUrl;
    private boolean active;
}
