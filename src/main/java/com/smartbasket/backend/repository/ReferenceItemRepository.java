package com.smartbasket.backend.repository;

import com.smartbasket.backend.model.ReferenceItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReferenceItemRepository extends MongoRepository<ReferenceItem, String> {
    List<ReferenceItem> findByCategory(String category);
    List<ReferenceItem> findByCategoryId(String categoryId);
    List<ReferenceItem> findByNameContainingIgnoreCase(String name);
    Optional<ReferenceItem> findByNameIgnoreCase(String name);
    
    // Bulk fetch by names (case-insensitive) for performance optimization
    @Query("{ 'name': { $in: ?0 } }")
    List<ReferenceItem> findByNameIn(Collection<String> names);
    
    // Query items across multiple categories (parent + subcategories)
    List<ReferenceItem> findByCategoryIdIn(List<String> categoryIds);
    // Paginated queries
    Page<ReferenceItem> findByCategoryId(String categoryId, Pageable pageable);
    Page<ReferenceItem> findByNameContainingIgnoreCase(String name, Pageable pageable);
    Page<ReferenceItem> findByCategoryIdIn(List<String> categoryIds, Pageable pageable);

    // Search by barcode
    Optional<ReferenceItem> findByBarcode(String barcode);
}

