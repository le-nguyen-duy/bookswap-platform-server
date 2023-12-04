package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Order.OrderStatus;
import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Payment.Status;
import com.example.bookswapplatform.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Orders, UUID> {
    @Query("SELECT DISTINCT o from Orders o join o.payments p where o.createBy = :user and p.status = :status")
    Set<Orders> findByPaymentStatusAndCreateBy(User user, Status status);
    @Query("SELECT DISTINCT o from Orders o where o.createBy = :user and o.orderStatus = :orderStatus")
    Set<Orders> findByOrderStatusAndCreateBy(User user, OrderStatus orderStatus);
    @Query("SELECT DISTINCT o FROM Orders o JOIN o.orderDetails od JOIN od.book b WHERE b.createBy = :user AND o.createBy <> :user " +
            "AND o.orderStatus = :orderStatus ")
    Set<Orders> findOrdersWithBooksCreatedByUser(User user, OrderStatus orderStatus);
    @Query("SELECT DISTINCT o FROM Orders o JOIN o.payments pm JOIN o.post p " +
            "WHERE p.createBy = :user AND o.createBy <> :user AND pm.createBy = :email AND pm.status = :status")
    Set<Orders> findOrdersWithBooksCreatedByUserAndPaymentStatus(User user, String email, Status status);

    @Query("SELECT o from Orders o WHERE o.createBy = :user AND o.isPayment = false ")
    Set<Orders> findOrdersByCreateByAndPayment(User user);

    Set<Orders> findOrdersByCreateBy(User user);



}
