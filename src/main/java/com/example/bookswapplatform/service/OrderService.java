package com.example.bookswapplatform.service;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    ResponseEntity<BaseResponseDTO> createOrder(Principal principal, UUID postId, List<UUID> bookIds);
    ResponseEntity<BaseResponseDTO> getOrderDetailsById(UUID id);
    ResponseEntity<BaseResponseDTO> getAllRequestUserOrders(Principal principal);
    ResponseEntity<BaseResponseDTO> getAllUserOrdersNotConfirm(Principal principal);
    ResponseEntity<BaseResponseDTO> confirmOrder(UUID orderId);
    ResponseEntity<BaseResponseDTO> getRequestUserOrdersNotPay(Principal principal);

}
