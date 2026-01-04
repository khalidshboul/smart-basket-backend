package com.smartbasket.backend.repository;

import com.smartbasket.backend.model.StorePrice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StorePriceRepository extends MongoRepository<StorePrice, String> {
    List<StorePrice> findByStoreItemId(String storeItemId);
    List<StorePrice> findByStoreItemIdOrderByTimestampDesc(String storeItemId);
}
