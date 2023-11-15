package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Payment.Payment;
import com.example.bookswapplatform.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    @Query("SELECT p from Payment p JOIN p.orders o WHERE p.orders = :orders AND p.createBy = :user AND o.isPayment = false ")
    Optional<Payment> getPaymentsByOrders (Orders orders, User user);
}
