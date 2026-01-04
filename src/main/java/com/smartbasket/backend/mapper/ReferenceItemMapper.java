package com.smartbasket.backend.mapper;

import com.smartbasket.backend.dto.CreateReferenceItemRequest;
import com.smartbasket.backend.dto.ReferenceItemDto;
import com.smartbasket.backend.model.ReferenceItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class ReferenceItemMapper {

    public ReferenceItemDto toDto(ReferenceItem entity) {
        if (entity == null) {
            return null;
        }
        return ReferenceItemDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .nameAr(entity.getNameAr())
                .categoryId(entity.getCategoryId())
                .category(entity.getCategory())
                .description(entity.getDescription())
                .descriptionAr(entity.getDescriptionAr())
                .images(entity.getImages() != null ? entity.getImages() : new ArrayList<>())
                .availableInAllStores(entity.isAvailableInAllStores())
                .specificStoreIds(entity.getSpecificStoreIds() != null ? entity.getSpecificStoreIds() : new ArrayList<>())
                .active(entity.isActive())
                .build();
    }

    public ReferenceItem toEntity(CreateReferenceItemRequest request) {
        if (request == null) {
            return null;
        }
        return ReferenceItem.builder()
                .name(request.getName())
                .nameAr(request.getNameAr())
                .categoryId(request.getCategoryId())
                .description(request.getDescription())
                .descriptionAr(request.getDescriptionAr())
                .images(request.getImages() != null ? request.getImages() : new ArrayList<>())
                .availableInAllStores(request.getAvailableInAllStores() != null ? request.getAvailableInAllStores() : true)
                .specificStoreIds(request.getSpecificStoreIds() != null ? request.getSpecificStoreIds() : new ArrayList<>())
                .build();
    }
}

