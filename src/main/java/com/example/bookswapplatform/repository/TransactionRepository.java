package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Payment.Transaction.Transaction;
import com.example.bookswapplatform.entity.Payment.Transaction.TransactionType;
import com.example.bookswapplatform.entity.Payment.UserWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findAllByCreateByOrToWallet(String email, UserWallet userWallet);

    List<Transaction> findAllByCreateBy(String email);

    @Query("SELECT t FROM Transaction t WHERE t.createBy = :email AND t.transactionType IN (:types)")
    List<Transaction> findAllByCreateByAndTransactionType(@Param("email") String email, @Param("types") List<TransactionType> types);

    List<Transaction> findAllByToWalletAndTransactionType(UserWallet userWallet, TransactionType transactionType);
}
