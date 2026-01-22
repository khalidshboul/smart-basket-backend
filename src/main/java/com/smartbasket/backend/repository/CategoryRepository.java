package com.smartbasket.backend.repository;

import com.smartbasket.backend.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {
    List<Category> findByActiveTrueOrderByDisplayOrderAsc();
    List<Category> findAllByOrderByDisplayOrderAsc();
    Optional<Category> findByNameIgnoreCase(String name);
    Optional<Category> findByNameArIgnoreCase(String nameAr);
    boolean existsByNameIgnoreCase(String name);
    
    // Top-level categories (no parent)
    List<Category> findByParentCategoryIdIsNullAndActiveTrueOrderByDisplayOrderAsc();
    List<Category> findByParentCategoryIdIsNullOrderByDisplayOrderAsc();
    
    // Subcategories of a parent
    List<Category> findByParentCategoryIdAndActiveTrueOrderByDisplayOrderAsc(String parentCategoryId);
    List<Category> findByParentCategoryIdOrderByDisplayOrderAsc(String parentCategoryId);
    
    // Count subcategories
    long countByParentCategoryId(String parentCategoryId);
    
    // Check if category has subcategories
    boolean existsByParentCategoryId(String parentCategoryId);
    
    // Find child category by parent and name (for hierarchical path lookup)
    Optional<Category> findByParentCategoryIdAndNameIgnoreCase(String parentCategoryId, String name);
    Optional<Category> findByParentCategoryIdAndNameArIgnoreCase(String parentCategoryId, String nameAr);
    
    // Find top-level category by name (null parent)
    Optional<Category> findByParentCategoryIdIsNullAndNameIgnoreCase(String name);
    Optional<Category> findByParentCategoryIdIsNullAndNameArIgnoreCase(String nameAr);
}
