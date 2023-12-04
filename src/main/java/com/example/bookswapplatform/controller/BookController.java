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
import java.util.UUID;

@RestController
@RequestMapping("api/v1/book")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_USER')")
public class BookController {
    private final BookService bookService;

    @PostMapping("/create")
    public ResponseEntity<BaseResponseDTO> createBook (Principal principal, @Valid @RequestBody BookRequest bookRequest) {
            return bookService.createBook(principal.getName(), bookRequest);
    }

    @GetMapping("/title")
    public ResponseEntity<BaseResponseDTO> getBookByTitle (@RequestParam String title) {
        return bookService.findByTitle(title);

    }
    @PostMapping("/image")
    public ResponseEntity<BaseResponseDTO> setImage (@RequestParam UUID bookId, @RequestBody BookImageDTO bookImageDTO) {
        return bookService.setImageBook(bookId, bookImageDTO);
    }

    @GetMapping("/available")
    public ResponseEntity<BaseResponseDTO> getUserBooksAvailable (Principal principal,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "3") int size) {
        return bookService.getUserBooksAvailable(principal, page, size);
    }

    @GetMapping("/is-done")
    public ResponseEntity<BaseResponseDTO> getUserBooksIsDone (Principal principal,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "3") int size) {
        return bookService.getUserBooksIsDone(principal, page, size);
    }

    @GetMapping("/in-post")
    public ResponseEntity<BaseResponseDTO> getUserBooksInPost (Principal principal,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "3") int size) {
        return bookService.getUserBooksInPost(principal, page, size);
    }
    @PutMapping("/modify")
    public ResponseEntity<BaseResponseDTO> modifyBook(Principal principal,
                                                      @RequestParam UUID bookId,
                                                      @Valid @RequestBody BookRequest bookRequest) {
        return bookService.modifyBook(principal, bookId, bookRequest);
    }
    @DeleteMapping("/delete")
    public ResponseEntity<BaseResponseDTO> deleteBook(@RequestParam UUID bookId) {
        return bookService.deleteBook(bookId);
    }
    @GetMapping("id")
    public ResponseEntity<BaseResponseDTO> findBookById(@RequestParam UUID bookId) {
        return bookService.findById(bookId);
    }
}
