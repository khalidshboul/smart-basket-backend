package com.smartbasket.backend.mapper;

import com.smartbasket.backend.dto.CreateStoreRequest;
import com.smartbasket.backend.dto.StoreDto;
import com.smartbasket.backend.model.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public StoreDto toDto(Store entity) {
        if (entity == null) {
            return null;
        }
        return StoreDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .nameAr(entity.getNameAr())
                .location(entity.getLocation())
                .locationAr(entity.getLocationAr())
                .logoUrl(entity.getLogoUrl())
                .active(entity.isActive())
                .build();
    }

    public Store toEntity(CreateStoreRequest request) {
        if (request == null) {
            return null;
        }
        return Store.builder()
                .name(request.getName())
                .nameAr(request.getNameAr())
                .location(request.getLocation())
                .locationAr(request.getLocationAr())
                .logoUrl(request.getLogoUrl())
                .active(true)
                .build();
    }
}
