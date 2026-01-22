package com.smartbasket.backend.repository;

import com.smartbasket.backend.model.StoreItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreItemRepository extends MongoRepository<StoreItem, String> {
    List<StoreItem> findByReferenceItemId(String referenceItemId);
    List<StoreItem> findByStoreId(String storeId);
    Optional<StoreItem> findByStoreIdAndReferenceItemId(String storeId, String referenceItemId);
    
    // For cascade deletion when stores are unassigned from a reference item
    void deleteByReferenceItemIdAndStoreIdIn(String referenceItemId, List<String> storeIds);
}
