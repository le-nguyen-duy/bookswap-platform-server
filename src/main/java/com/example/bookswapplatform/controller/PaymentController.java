package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/payment")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;
    @PostMapping("/checkout-request")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> checkoutForUserRequest (@RequestParam UUID paymentId) {
        return paymentService.checkoutForUserRequest(paymentId);
    }

    @PostMapping("/checkout-get-request")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> checkoutForUserGetRequest (@RequestParam UUID paymentId) {
        return paymentService.checkoutForUserGetRequest(paymentId);
    }
}
