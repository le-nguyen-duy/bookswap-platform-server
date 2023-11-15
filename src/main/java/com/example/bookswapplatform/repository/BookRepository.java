package com.example.bookswapplatform.repository;

import com.example.bookswapplatform.entity.Book.Book;
import com.example.bookswapplatform.entity.User.User;
import org.hibernate.annotations.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {
    Optional<Book> findByTitle (String title);
    Optional<Book> findById (UUID id);
    @Query("SELECT b from Book b where b.createBy = :user and b.isDone = false and b.post is null")
//    @Filter(name = "notDeleted")
    Page<Book> findBookAvailable (User user, Pageable pageable);
    @Query("SELECT b from Book b where b.createBy = :user and b.isDone = true")
//    @Filter(name = "notDeleted")
    Page<Book> findBookIsDone (User user, Pageable pageable);
    @Query("SELECT b from Book b where b.createBy = :user and b.post is not null")
//    @Filter(name = "notDeleted")
    Page<Book> findBookInPost (User user, Pageable pageable);
    @Query("SELECT b from Book b where b.createBy = :user and b.isDone = false and b.post is null")
    Set<Book> findByCreatedBy(User user);



}
