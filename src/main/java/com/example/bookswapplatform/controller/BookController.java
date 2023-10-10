package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.BookDTO;
import com.example.bookswapplatform.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("api/v1/book")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> createBook (Principal principal, @RequestBody BookDTO bookDTO) {
        return bookService.createBook(principal.getName(),bookDTO);
    }

    @GetMapping("/title")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getBookByTitle (@RequestParam String title) {
        return bookService.findByTitle(title);

    }
}
