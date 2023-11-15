package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.service.OrderService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> createOrder (Principal principal, @RequestParam UUID postId, @RequestParam List<UUID> bookIds) {
        return orderService.createOrder(principal,postId, bookIds);
    }

    @GetMapping("/request")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getAllRequestUserOrder(Principal principal) {
        return orderService.getAllRequestUserOrders(principal);
    }

    @GetMapping("/take")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getAllUserOrder(Principal principal) {
        return orderService.getAllUserOrdersNotConfirm(principal);
    }
    @PostMapping("/confirm")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> confirmOrder(@RequestParam UUID orderId) {
        return orderService.confirmOrder(orderId);
    }
}
