package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Area.Area;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AreaRepository extends JpaRepository<Area, UUID> {
    Optional<Area> findByCity(String city);
}
