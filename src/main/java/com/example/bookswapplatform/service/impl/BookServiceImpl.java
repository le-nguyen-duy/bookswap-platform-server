package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.BookDTO;
import com.example.bookswapplatform.entity.Book.Author;
import com.example.bookswapplatform.entity.Book.Book;
import com.example.bookswapplatform.entity.Book.BookImage;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.AuthorsRepository;
import com.example.bookswapplatform.repository.BookImageRepository;
import com.example.bookswapplatform.repository.BookRepository;
import com.example.bookswapplatform.repository.UserRepository;
import com.example.bookswapplatform.service.BookService;
import com.google.firebase.auth.FirebaseAuth;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final ModelMapper modelMapper;
    private final BookRepository bookRepository;
    private final AuthorsRepository authorsRepository;
    private final BookImageRepository bookImageRepository;
    private final UserRepository userRepository;


    @Override
    public ResponseEntity<BaseResponseDTO> createBook(String uid, BookDTO bookDTO) {
        Book book = modelMapper.map(bookDTO, Book.class);
        User user = userRepository.findByFireBaseUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        book.setCreatedBy(user.getEmail());
        // tạo authors mới nếu chứ có
        Set<Author> authors = new HashSet<>();
        for (String authorName : bookDTO.getAuthors()
        ) {
            Author author = authorsRepository.findByName(authorName).orElseGet(() -> {
                Author newAuthor = new Author();
                newAuthor.setName(authorName);
                return authorsRepository.save(newAuthor);
            });
            authors.add(author);
        }
        book.setAuthors(authors);
        // set image cho book
        Set<BookImage> bookImages = new HashSet<>();
        for (String img : bookDTO.getImage()
        ) {
            BookImage bookImage = new BookImage();
            bookImage.setImage(img);
            bookImageRepository.save(bookImage);
            bookImages.add(bookImage);
        }
        book.setBookImages(bookImages);

        bookRepository.save(book);

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.CREATED, "Create Successfully"));

    }

    @Override
    public ResponseEntity<BaseResponseDTO> findByTitle(String title) {
        Book book = bookRepository.findByTitle(title)
                .orElseThrow(()->new ResourceNotFoundException("Book with title :"+title+" Not Found!"));
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully", book));
    }
}
