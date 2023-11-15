package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Orders, UUID> {
    Set<Orders> findByCreateBy(User user);
    @Query("SELECT DISTINCT o FROM Orders o JOIN o.orderDetails od JOIN od.book b WHERE b.createBy = :user AND o.createBy <> :user " +
            "AND o.isConfirm = false ")
    Set<Orders> findOrdersWithBooksCreatedByUser(User user);

    @Query("SELECT o from Orders o WHERE o.createBy = :user AND o.isPayment = false ")
    Set<Orders> findOrdersByCreateByAndPayment(User user);



}
