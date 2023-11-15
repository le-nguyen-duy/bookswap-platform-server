package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Payment.Transaction.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
}
