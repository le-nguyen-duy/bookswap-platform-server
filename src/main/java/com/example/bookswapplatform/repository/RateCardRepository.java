package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.User.RateCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RateCardRepository extends JpaRepository<RateCard, UUID> {
}
