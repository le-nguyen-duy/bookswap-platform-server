package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Authority;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PermissionRepository extends JpaRepository<Authority, UUID> {
    Optional<Authority> findByName (String name);
}
