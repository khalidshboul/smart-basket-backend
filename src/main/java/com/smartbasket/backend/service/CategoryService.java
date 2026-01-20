package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.CategoryDto;
import com.smartbasket.backend.dto.CreateCategoryRequest;
import com.smartbasket.backend.exception.ResourceNotFoundException;
import com.smartbasket.backend.model.Category;
import com.smartbasket.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get only top-level categories (no parent)
     */
    public List<CategoryDto> getTopLevelCategories() {
        return categoryRepository.findByParentCategoryIdIsNullOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get only active top-level categories
     */
    public List<CategoryDto> getActiveTopLevelCategories() {
        return categoryRepository.findByParentCategoryIdIsNullAndActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get subcategories of a parent category
     */
    public List<CategoryDto> getSubcategories(String parentCategoryId) {
        return categoryRepository.findByParentCategoryIdOrderByDisplayOrderAsc(parentCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get active subcategories of a parent category
     */
    public List<CategoryDto> getActiveSubcategories(String parentCategoryId) {
        return categoryRepository.findByParentCategoryIdAndActiveTrueOrderByDisplayOrderAsc(parentCategoryId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get category with its subcategories populated
     */
    public Optional<CategoryDto> getCategoryWithSubcategories(String id) {
        return categoryRepository.findById(id)
                .map(category -> {
                    CategoryDto dto = toDto(category);
                    List<CategoryDto> subcategories = getSubcategories(id);
                    dto.setSubcategories(subcategories);
                    return dto;
                });
    }

    public Optional<CategoryDto> getCategoryById(String id) {
        return categoryRepository.findById(id)
                .map(this::toDto);
    }

    public CategoryDto createCategory(CreateCategoryRequest request) {
        // Check for duplicate name
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        }

        // Validate parent category exists if specified
        if (request.getParentCategoryId() != null) {
            if (!categoryRepository.existsById(request.getParentCategoryId())) {
                throw new ResourceNotFoundException("Parent category not found: " + request.getParentCategoryId());
            }
        }

        Category category = Category.builder()
                .name(request.getName())
                .nameAr(request.getNameAr())
                .icon(request.getIcon())
                .description(request.getDescription())
                .descriptionAr(request.getDescriptionAr())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .parentCategoryId(request.getParentCategoryId())
                .subcategoryIds(new ArrayList<>())
                .build();

        Category saved = categoryRepository.save(category);
        
        // Add child ID to parent's subcategoryIds list
        if (saved.getParentCategoryId() != null) {
            categoryRepository.findById(saved.getParentCategoryId())
                    .ifPresent(parent -> {
                        if (parent.getSubcategoryIds() == null) {
                            parent.setSubcategoryIds(new ArrayList<>());
                        }
                        parent.getSubcategoryIds().add(saved.getId());
                        categoryRepository.save(parent);
                    });
        }
        
        return toDto(saved);
    }

    public CategoryDto updateCategory(String id, CreateCategoryRequest request) {
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        // Check for duplicate name (excluding current category)
        categoryRepository.findByNameIgnoreCase(request.getName())
                .ifPresent(found -> {
                    if (!found.getId().equals(id)) {
                        throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
                    }
                });

        // Validate parent category if specified
        if (request.getParentCategoryId() != null) {
            // Prevent circular reference
            if (request.getParentCategoryId().equals(id)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            if (!categoryRepository.existsById(request.getParentCategoryId())) {
                throw new ResourceNotFoundException("Parent category not found: " + request.getParentCategoryId());
            }
        }

        // Handle parent change - update subcategoryIds lists
        String oldParentId = existing.getParentCategoryId();
        String newParentId = request.getParentCategoryId();
        boolean parentChanged = (oldParentId == null && newParentId != null) ||
                                (oldParentId != null && !oldParentId.equals(newParentId));
        
        if (parentChanged) {
            // Remove from old parent's subcategoryIds
            if (oldParentId != null) {
                categoryRepository.findById(oldParentId)
                        .ifPresent(oldParent -> {
                            if (oldParent.getSubcategoryIds() != null) {
                                oldParent.getSubcategoryIds().remove(id);
                                categoryRepository.save(oldParent);
                            }
                        });
            }
            // Add to new parent's subcategoryIds
            if (newParentId != null) {
                categoryRepository.findById(newParentId)
                        .ifPresent(newParent -> {
                            if (newParent.getSubcategoryIds() == null) {
                                newParent.setSubcategoryIds(new ArrayList<>());
                            }
                            if (!newParent.getSubcategoryIds().contains(id)) {
                                newParent.getSubcategoryIds().add(id);
                                categoryRepository.save(newParent);
                            }
                        });
            }
        }

        existing.setName(request.getName());
        existing.setNameAr(request.getNameAr());
        existing.setIcon(request.getIcon());
        existing.setDescription(request.getDescription());
        existing.setDescriptionAr(request.getDescriptionAr());
        existing.setParentCategoryId(request.getParentCategoryId());
        if (request.getDisplayOrder() != null) {
            existing.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getActive() != null) {
            existing.setActive(request.getActive());
        }

        Category saved = categoryRepository.save(existing);
        return toDto(saved);
    }

    public boolean deleteCategory(String id) {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null) {
            return false;
        }
        
        // Check if category has subcategories - prevent deletion
        if (categoryRepository.existsByParentCategoryId(id)) {
            throw new IllegalArgumentException("Cannot delete category with subcategories. Delete subcategories first.");
        }
        
        // Remove from parent's subcategoryIds list
        if (category.getParentCategoryId() != null) {
            categoryRepository.findById(category.getParentCategoryId())
                    .ifPresent(parent -> {
                        if (parent.getSubcategoryIds() != null) {
                            parent.getSubcategoryIds().remove(id);
                            categoryRepository.save(parent);
                        }
                    });
        }
        
        categoryRepository.deleteById(id);
        return true;
    }

    public Optional<CategoryDto> toggleStatus(String id) {
        return categoryRepository.findById(id)
                .map(existing -> {
                    existing.setActive(!existing.isActive());
                    return categoryRepository.save(existing);
                })
                .map(this::toDto);
    }

    private CategoryDto toDto(Category entity) {
        String parentName = null;
        if (entity.getParentCategoryId() != null) {
            parentName = categoryRepository.findById(entity.getParentCategoryId())
                    .map(Category::getName)
                    .orElse(null);
        }
        
        int subcategoryCount = entity.getSubcategoryIds() != null ? 
                entity.getSubcategoryIds().size() : 
                (int) categoryRepository.countByParentCategoryId(entity.getId());
        
        return CategoryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .nameAr(entity.getNameAr())
                .icon(entity.getIcon())
                .description(entity.getDescription())
                .descriptionAr(entity.getDescriptionAr())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.isActive())
                .parentCategoryId(entity.getParentCategoryId())
                .parentCategoryName(parentName)
                .subcategoryCount(subcategoryCount)
                .subcategoryIds(entity.getSubcategoryIds() != null ? entity.getSubcategoryIds() : new ArrayList<>())
                .build();
    }
}
