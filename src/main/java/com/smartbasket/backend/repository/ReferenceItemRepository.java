package com.smartbasket.backend.repository;

import com.smartbasket.backend.model.ReferenceItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReferenceItemRepository extends MongoRepository<ReferenceItem, String> {
    List<ReferenceItem> findByCategory(String category);
    List<ReferenceItem> findByCategoryId(String categoryId);
    List<ReferenceItem> findByNameContainingIgnoreCase(String name);
}

