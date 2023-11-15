package com.example.bookswapplatform.service;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface PaymentService {
//    ResponseEntity<BaseResponseDTO> getPayment(UUID orderId);

    ResponseEntity<BaseResponseDTO> checkoutForUserRequest(UUID paymentId);
    ResponseEntity<BaseResponseDTO> checkoutForUserGetRequest(UUID paymentId);
}
