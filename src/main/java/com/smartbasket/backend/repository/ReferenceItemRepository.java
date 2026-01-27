package com.smartbasket.backend.repository;

import com.smartbasket.backend.model.ReferenceItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceItemRepository extends MongoRepository<ReferenceItem, String> {
    List<ReferenceItem> findByCategory(String category);
    List<ReferenceItem> findByCategoryId(String categoryId);
    List<ReferenceItem> findByNameContainingIgnoreCase(String name);
    Optional<ReferenceItem> findByNameIgnoreCase(String name);
    
    // Query items across multiple categories (parent + subcategories)
    List<ReferenceItem> findByCategoryIdIn(List<String> categoryIds);
    
    // Search by barcode
    Optional<ReferenceItem> findByBarcode(String barcode);
}

