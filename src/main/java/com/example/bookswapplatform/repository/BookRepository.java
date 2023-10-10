package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Book.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {
    Optional<Book> findByTitle (String title);
}
