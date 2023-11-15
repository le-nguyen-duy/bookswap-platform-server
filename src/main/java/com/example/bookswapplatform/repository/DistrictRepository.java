package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Area.District;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DistrictRepository extends JpaRepository<District, UUID> {
    Optional<District> findByDistrict(String district);
}
