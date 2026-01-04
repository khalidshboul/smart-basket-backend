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
public class CreateStoreRequest {
    @NotBlank(message = "Name is required")
    private String name;
    private String nameAr;

    private String location;
    private String locationAr;
    private String logoUrl;
}
