package com.smartbasket.backend.mapper;

import com.smartbasket.backend.dto.CreateReferenceItemRequest;
import com.smartbasket.backend.dto.ReferenceItemDto;
import com.smartbasket.backend.model.Category;
import com.smartbasket.backend.model.ReferenceItem;
import com.smartbasket.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReferenceItemMapper {

    private final CategoryRepository categoryRepository;

    /**
     * Converts a single entity to DTO (queries DB for category breadcrumb).
     * Use for single-item endpoints (getById, create, update, toggleStatus).
     */
    public ReferenceItemDto toDto(ReferenceItem entity) {
        if (entity == null) {
            return null;
        }
        
        String breadcrumb = buildCategoryBreadcrumb(entity.getCategoryId());
        return buildDto(entity, breadcrumb);
    }

    /**
     * Batch-converts entities to DTOs using a pre-fetched category cache.
     * Eliminates N+1 queries for list endpoints.
     */
    public List<ReferenceItemDto> toDtoList(List<ReferenceItem> entities) {
        if (entities == null || entities.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, Category> categoryCache = loadCategoryCache();

        return entities.stream()
                .map(entity -> {
                    String breadcrumb = buildBreadcrumbFromCache(entity.getCategoryId(), categoryCache);
                    return buildDto(entity, breadcrumb);
                })
                .collect(Collectors.toList());
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

    // ──────────────────────────────────────────────
    // Category Cache
    // ──────────────────────────────────────────────

    /**
     * Loads all categories into an id-keyed map (single query).
     */
    private Map<String, Category> loadCategoryCache() {
        return categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, c -> c, (a, b) -> a));
    }

    private String buildBreadcrumbFromCache(String categoryId, Map<String, Category> cache) {
        if (categoryId == null) {
            return "";
        }

        Category category = cache.get(categoryId);
        if (category == null) {
            return "";
        }

        if (category.getParentCategoryId() != null) {
            Category parent = cache.get(category.getParentCategoryId());
            if (parent != null) {
                return parent.getName() + " > " + category.getName();
            }
        }
        return category.getName();
    }

    /**
     * Fallback for single-item lookups (queries DB directly).
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

    // ──────────────────────────────────────────────
    // DTO Builder
    // ──────────────────────────────────────────────

    private ReferenceItemDto buildDto(ReferenceItem entity, String breadcrumb) {
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
}
