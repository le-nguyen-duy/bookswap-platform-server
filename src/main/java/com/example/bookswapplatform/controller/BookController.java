package com.example.bookswapplatform.controller;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.BookImageDTO;
import com.example.bookswapplatform.dto.BookRequest;
import com.example.bookswapplatform.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.text.ParseException;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/book")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('BOOK:CREATE')")
    public ResponseEntity<BaseResponseDTO> createBook (Principal principal, @Valid @RequestBody BookRequest bookRequest) {
            return bookService.createBook(principal.getName(), bookRequest);
    }

    @GetMapping("/title")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getBookByTitle (@RequestParam String title) {
        return bookService.findByTitle(title);

    }
    @PostMapping("/image")
    @PreAuthorize("hasAuthority('BOOK:MODIFY')")
    public ResponseEntity<BaseResponseDTO> setImage (@RequestParam UUID bookId, @RequestBody BookImageDTO bookImageDTO) {
        return bookService.setImageBook(bookId, bookImageDTO);
    }

    @GetMapping("/available")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getUserBooksAvailable (Principal principal,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "3") int size) {
        return bookService.getUserBooksAvailable(principal, page, size);
    }

    @GetMapping("/is-done")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getUserBooksIsDone (Principal principal,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "3") int size) {
        return bookService.getUserBooksIsDone(principal, page, size);
    }

    @GetMapping("/in-post")
    @PreAuthorize("hasAuthority('BOOK:READ')")
    public ResponseEntity<BaseResponseDTO> getUserBooksInPost (Principal principal,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "3") int size) {
        return bookService.getUserBooksInPost(principal, page, size);
    }
    @PutMapping("/modify")
    @PreAuthorize("hasAuthority('BOOK:MODIFY')")
    public ResponseEntity<BaseResponseDTO> modifyBook(Principal principal,
                                                      @RequestParam UUID bookId,
                                                      @Valid @RequestBody BookRequest bookRequest) {
        return bookService.modifyBook(principal, bookId, bookRequest);
    }
    @DeleteMapping("/delete")
    @PreAuthorize("hasAuthority('BOOK:DELETE')")
    public ResponseEntity<BaseResponseDTO> deleteBook(@RequestParam UUID bookId) {
        return bookService.deleteBook(bookId);
    }
}
