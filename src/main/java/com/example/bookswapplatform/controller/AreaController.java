package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.service.AreaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/area")
@RequiredArgsConstructor
public class AreaController {
    private final AreaService areaService;
    @GetMapping("/all")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getAll() {
        return areaService.getAllArea();
    }
}
