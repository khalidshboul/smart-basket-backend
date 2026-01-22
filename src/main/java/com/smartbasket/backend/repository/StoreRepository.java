package com.smartbasket.backend.repository;

import com.smartbasket.backend.model.Store;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreRepository extends MongoRepository<Store, String> {
    List<Store> findByActiveTrue();
    Optional<Store> findByNameIgnoreCase(String name);
    Optional<Store> findByNameArIgnoreCase(String nameAr);
}
