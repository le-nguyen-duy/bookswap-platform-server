package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.security.firebase.service.UserManagementService;
import com.google.firebase.auth.FirebaseAuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final UserManagementService userManagementService;

    @PostMapping("/user-claims")
    public ResponseEntity<String> setUserClaims(Principal principal) throws FirebaseAuthException {
        String uid = principal.getName();
        userManagementService.setUserClaims(uid);
        return ResponseEntity.ok("Successful");
    }
}
