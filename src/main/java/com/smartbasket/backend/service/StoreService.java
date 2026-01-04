package com.smartbasket.backend.service;

import com.smartbasket.backend.dto.CreateStoreRequest;
import com.smartbasket.backend.dto.StoreDto;
import com.smartbasket.backend.mapper.StoreMapper;
import com.smartbasket.backend.model.Store;
import com.smartbasket.backend.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;

    public List<StoreDto> getAllStores() {
        return storeRepository.findAll()
                .stream()
                .map(storeMapper::toDto)
                .collect(Collectors.toList());
    }

    public List<StoreDto> getActiveStores() {
        return storeRepository.findByActiveTrue()
                .stream()
                .map(storeMapper::toDto)
                .collect(Collectors.toList());
    }

    public Optional<StoreDto> getStoreById(String id) {
        return storeRepository.findById(id)
                .map(storeMapper::toDto);
    }

    public StoreDto createStore(CreateStoreRequest request) {
        Store entity = storeMapper.toEntity(request);
        Store saved = storeRepository.save(entity);
        return storeMapper.toDto(saved);
    }

    public Optional<StoreDto> updateStore(String id, CreateStoreRequest request) {
        return storeRepository.findById(id)
                .map(existing -> {
                    existing.setName(request.getName());
                    existing.setNameAr(request.getNameAr());
                    existing.setLocation(request.getLocation());
                    existing.setLocationAr(request.getLocationAr());
                    existing.setLogoUrl(request.getLogoUrl());
                    return storeRepository.save(existing);
                })
                .map(storeMapper::toDto);
    }

    public Optional<StoreDto> toggleStoreStatus(String id) {
        return storeRepository.findById(id)
                .map(existing -> {
                    existing.setActive(!existing.isActive());
                    return storeRepository.save(existing);
                })
                .map(storeMapper::toDto);
    }

    public boolean deleteStore(String id) {
        if (storeRepository.existsById(id)) {
            storeRepository.deleteById(id);
            return true;
        }
        return false;
    }
}
