package com.example.bookswapplatform.service;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.BookDTO;
import org.springframework.http.ResponseEntity;

public interface BookService {
    ResponseEntity<BaseResponseDTO> createBook (String uid, BookDTO bookDTO);

    ResponseEntity<BaseResponseDTO> findByTitle (String title);
}
