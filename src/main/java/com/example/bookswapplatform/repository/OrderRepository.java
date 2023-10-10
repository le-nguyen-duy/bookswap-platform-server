package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Order.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Orders, UUID> {
}
