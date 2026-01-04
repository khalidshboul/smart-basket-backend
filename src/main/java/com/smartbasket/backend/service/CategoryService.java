package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.CategoryDto;
import com.smartbasket.backend.dto.CreateCategoryRequest;
import com.smartbasket.backend.exception.ResourceNotFoundException;
import com.smartbasket.backend.model.Category;
import com.smartbasket.backend.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public Optional<CategoryDto> getCategoryById(String id) {
        return categoryRepository.findById(id)
                .map(this::toDto);
    }

    public CategoryDto createCategory(CreateCategoryRequest request) {
        // Check for duplicate name
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new IllegalArgumentException("Category with name '" + request.getName() + "' already exists");
        }

        Category category = Category.builder()
                .name(request.getName())
                .nameAr(request.getNameAr())
                .icon(request.getIcon())
                .description(request.getDescription())
                .descriptionAr(request.getDescriptionAr())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        Category saved = categoryRepository.save(category);
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

        existing.setName(request.getName());
        existing.setNameAr(request.getNameAr());
        existing.setIcon(request.getIcon());
        existing.setDescription(request.getDescription());
        existing.setDescriptionAr(request.getDescriptionAr());
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
        if (categoryRepository.existsById(id)) {
            categoryRepository.deleteById(id);
            return true;
        }
        return false;
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
        return CategoryDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .nameAr(entity.getNameAr())
                .icon(entity.getIcon())
                .description(entity.getDescription())
                .descriptionAr(entity.getDescriptionAr())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.isActive())
                .build();
    }
}
