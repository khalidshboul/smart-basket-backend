package com.smartbasket.backend.mapper;

import com.smartbasket.backend.dto.CreateReferenceItemRequest;
import com.smartbasket.backend.dto.ReferenceItemDto;
import com.smartbasket.backend.model.Category;
import com.smartbasket.backend.model.ReferenceItem;
import com.smartbasket.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class ReferenceItemMapper {

    private final CategoryRepository categoryRepository;

    public ReferenceItemDto toDto(ReferenceItem entity) {
        if (entity == null) {
            return null;
        }
        
        String breadcrumb = buildCategoryBreadcrumb(entity.getCategoryId());
        
        return ReferenceItemDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .nameAr(entity.getNameAr())
                .categoryId(entity.getCategoryId())
                .category(entity.getCategory())
                .categoryBreadcrumb(breadcrumb)
                .description(entity.getDescription())
                .descriptionAr(entity.getDescriptionAr())
                .images(entity.getImages() != null ? entity.getImages() : new ArrayList<>())
                .barcode(entity.getBarcode())
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
                .barcode(request.getBarcode())
                .availableInAllStores(request.getAvailableInAllStores() != null ? request.getAvailableInAllStores() : true)
                .specificStoreIds(request.getSpecificStoreIds() != null ? request.getSpecificStoreIds() : new ArrayList<>())
                .build();
    }
    
    /**
     * Build breadcrumb path for category (e.g., "Dairy > Milk")
     */
    private String buildCategoryBreadcrumb(String categoryId) {
        if (categoryId == null) {
            return "";
        }
        
        Category category = categoryRepository.findById(categoryId).orElse(null);
        if (category == null) {
            return "";
        }
        
        if (category.getParentCategoryId() != null) {
            Category parent = categoryRepository.findById(category.getParentCategoryId()).orElse(null);
            if (parent != null) {
                return parent.getName() + " > " + category.getName();
            }
        }
        return category.getName();
    }
}

