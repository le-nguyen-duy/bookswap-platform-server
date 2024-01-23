package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.service.BookSystemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/book-system")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN')")
public class BookSystemController {
    private final BookSystemService bookSystemService;
    @PostMapping ("/title")
    public ResponseEntity<BaseResponseDTO> getBookByTitle (@RequestParam String title) {
        return bookSystemService.getBookByTitle(title);

    }
    @GetMapping("/all")
    public ResponseEntity<BaseResponseDTO> getAll() {
        return bookSystemService.getAll();
    }
}
