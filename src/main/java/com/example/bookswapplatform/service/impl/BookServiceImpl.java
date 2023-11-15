package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.common.Pagination;
import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.BookDTO;
import com.example.bookswapplatform.dto.BookRequest;
import com.example.bookswapplatform.dto.BookImageDTO;
import com.example.bookswapplatform.entity.Book.*;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.*;
import com.example.bookswapplatform.service.BookService;
import com.example.bookswapplatform.utils.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.Condition;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Principal;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {
    private final ModelMapper modelMapper;
    private final BookRepository bookRepository;
    private final AuthorsRepository authorsRepository;
    private final BookImageRepository bookImageRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    @Override
    public ResponseEntity<BaseResponseDTO> createBook(String uid, BookRequest bookRequest)  {
        Book book = modelMapper.map(bookRequest, Book.class);
        User user = userRepository.findByFireBaseUid(uid)
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        book.setCreateBy(user);
        book.setPrice(BigDecimal.valueOf(0));
        int newPercent = Integer.parseInt(bookRequest.getNewPercent());
        if( newPercent >= 0 && newPercent <= 100 ) {
            book.setNewPercent(bookRequest.getNewPercent());

        } else {
            return ResponseEntity.badRequest()
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "New percent must be between 0 and 100"));
        }

        // tạo authors mới nếu chứ có
        Set<Author> authors = new HashSet<>();
        for (String authorName : bookRequest.getAuthors()
        ) {
            Author author = authorsRepository.findByName(authorName).orElseGet(() -> {
                Author newAuthor = new Author();
                newAuthor.setName(authorName);
                authors.add(newAuthor);
                return authorsRepository.save(newAuthor);
            });
            authors.add(author);
        }
        book.setAuthors(authors);
        try {
            book.setPublishedDate(DateTimeUtils.convertStringToLocalDate(bookRequest.getPublishDate()));
        } catch (ParseException e) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, e.getLocalizedMessage()));
        }
        // set category cho sách
        MainCategory mainCategory = categoryRepository.findByName(bookRequest.getCategory())
                .orElseThrow(()->new ResourceNotFoundException("Main category:"+bookRequest.getCategory()+" Not Found!"));
        book.setMainCategory(mainCategory);
        setSubCategoryAndSubSubCategory(book, bookRequest);

        bookRepository.save(book);

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.CREATED, "Create Successfully"));

    }
    private void setSubCategoryAndSubSubCategory(Book book, BookRequest bookRequest) {
        setSubCategory(book, bookRequest.getSubCategory(), book.getMainCategory().getSubCategories());
        Set<MainCategory> subSubCategories = new HashSet<>();
        for (MainCategory subCategory: book.getMainCategory().getSubCategories()
             ) {
            subSubCategories.addAll(subCategory.getSubSubCategories());
        }
        setSubSubCategory(book, bookRequest.getSubSubCategory(), subSubCategories);
    }

    private void setSubCategory(Book book, String subCategoryName, Set<MainCategory> subCategories) {
        if (subCategoryName != null) {
            for (MainCategory subCategory : subCategories) {
                if (subCategory.getName().equals(subCategoryName)) {
                    book.setSubCategory(subCategoryName);
                    return;
                }
            }
            throw new ResourceNotFoundException(subCategoryName + " not found");
        } else {
            book.setSubCategory(null);
        }
    }
    private void setSubSubCategory(Book book, String subCategoryName, Set<MainCategory> subCategories) {
        if (subCategoryName != null) {
            for (MainCategory subCategory : subCategories) {
                if (subCategory.getName().equals(subCategoryName)) {
                    book.setSubSubCategory(subCategoryName);
                    return;
                }
            }
            throw new ResourceNotFoundException(subCategoryName + " not found");
        } else {
            book.setSubSubCategory(null);
        }
    }

    @Override
    public ResponseEntity<BaseResponseDTO> findByTitle(String title) {
        Book book = bookRepository.findByTitle(title)
                .orElseThrow(()->new ResourceNotFoundException("Book with title :"+title+" Not Found!"));

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, convertToDTO(book)));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> setImageBook(UUID id, BookImageDTO bookImageDTO) {
        Book book = bookRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Book with id :"+id+" Not Found!"));
        // set image cho book
        for (String url : bookImageDTO.getUrl()
        ) {
            BookImage bookImage = new BookImage();
            bookImage.setImage(url);
            bookImage.setBook(book);
            bookImageRepository.save(bookImage);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully"));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getUserBooksAvailable(Principal principal, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber,pageSize);
        Page<Book> bookPage;
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(()->new ResourceNotFoundException("User Not Found"));
        // get book available
        bookPage = bookRepository.findBookAvailable(user, pageable);

        List<Book> books = bookPage.getContent();

        List<BookDTO> bookDTOS = books.stream().map(this::convertToDTO).toList();

        Pagination pagination = new Pagination(bookPage.getNumber(), bookPage.getTotalElements(), bookPage.getTotalPages());

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", pagination, bookDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getUserBooksIsDone(Principal principal, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber,pageSize);
        Page<Book> bookPage;
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(()->new ResourceNotFoundException("User Not Found"));
        // get book isDone
        bookPage = bookRepository.findBookIsDone(user, pageable);
        List<Book> books = bookPage.getContent();

        List<BookDTO> bookDTOS = books.stream().map(this::convertToDTO).toList();

        Pagination pagination = new Pagination(bookPage.getNumber(), bookPage.getTotalElements(), bookPage.getTotalPages());

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", pagination, bookDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getUserBooksInPost(Principal principal, int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber,pageSize);
        Page<Book> bookPage;
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(()->new ResourceNotFoundException("User Not Found"));
        // get book in post
        bookPage = bookRepository.findBookInPost(user, pageable);
        List<Book> books = bookPage.getContent();

        List<BookDTO> bookDTOS = books.stream().map(this::convertToDTO).toList();

        Pagination pagination = new Pagination(bookPage.getNumber(), bookPage.getTotalElements(), bookPage.getTotalPages());

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", pagination, bookDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> modifyBook(Principal principal, UUID bookId, BookRequest bookRequest) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(()->new ResourceNotFoundException("User Not Found"));
        Book book = bookRepository.findById(bookId).orElseThrow(()->new ResourceNotFoundException("Book with id :"+bookId+" Not Found!"));

        // Cấu hình Condition để giữ nguyên giá trị khi giống hoặc là null
        Condition<?, ?> skipNullAndSameValue = ctx ->
                ctx.getSource() != null && !ctx.getSource().equals(ctx.getDestination());

        // Ánh xạ giữa bookRequest và existingBook, sử dụng Condition
        modelMapper.typeMap(BookRequest.class, Book.class)
                .addMappings(mapper -> {
                    mapper.when(skipNullAndSameValue).map(BookRequest::getTitle, Book::setTitle);
                    mapper.when(skipNullAndSameValue).map(BookRequest::getDescription, Book::setDescription);
                    mapper.when(skipNullAndSameValue).map(BookRequest::getPublisher, Book::setPublisher);
                    mapper.when(skipNullAndSameValue).map(BookRequest::getIsbn, Book::setIsbn);
                    mapper.when(skipNullAndSameValue).map(BookRequest::getLanguage, Book::setLanguage);
                    mapper.when(skipNullAndSameValue).map(BookRequest::getNewPercent, Book::setNewPercent);
                    mapper.skip(Book::setAuthors);
                    mapper.skip(Book::setPublishedDate);
                    mapper.skip(Book::setMainCategory);
                    mapper.skip(Book::setSubCategory);
                    mapper.skip(Book::setSubSubCategory);
                    mapper.skip(Book::setPageCount);
                });
        modelMapper.map(bookRequest, book);
        book.setUpdateBy(user.getEmail());

        try {
            if(bookRequest.getPublishDate() != null &&
                    !DateTimeUtils.convertStringToLocalDate(bookRequest.getPublishDate()).equals(book.getPublishedDate())) {
                book.setPublishedDate(DateTimeUtils.convertStringToLocalDate(bookRequest.getPublishDate()));
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        if(bookRequest.getAuthors() != null) {
            Set<Author> authors = new HashSet<>();
            for (String authorName : bookRequest.getAuthors()
            ) {
                book.getAuthors().forEach(author -> {
                    if(!authorName.equals(author.getName())) {
                        Author bookRequestAuthor = authorsRepository.findByName(authorName).orElseGet(() -> {
                            Author newAuthor = new Author();
                            newAuthor.setName(authorName);
                            authors.add(newAuthor);
                            return authorsRepository.save(newAuthor);
                        });
                        authors.add(bookRequestAuthor);
                        book.setAuthors(authors);
                    }
                });

            }
        }
        if(bookRequest.getCategory() != null && !bookRequest.getCategory().equals(book.getMainCategory().getName())) {
            MainCategory mainCategory = categoryRepository.findByName(bookRequest.getCategory())
                    .orElseThrow(() -> new ResourceNotFoundException("Main category:" + bookRequest.getCategory() + " Not Found!"));
            book.setMainCategory(mainCategory);
        }
        if((bookRequest.getSubCategory() != null || bookRequest.getSubSubCategory() != null) &&
                (!bookRequest.getSubCategory().equals(book.getSubCategory()) || !Objects.equals(bookRequest.getSubSubCategory(),book.getSubSubCategory()))) {
            setSubCategoryAndSubSubCategory(book, bookRequest);
        }

        if(bookRequest.getPageCount() != 0 && bookRequest.getPageCount() != book.getPageCount()) {
            book.setPageCount(bookRequest.getPageCount());
        }
        bookRepository.save(book);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Modify Successfully"));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> deleteBook(UUID bookId) {
        Book book = bookRepository.findById(bookId).orElseThrow(()->new ResourceNotFoundException("Book with id :"+bookId+" Not Found!"));
        if(book.getPost() != null) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Book in a post please delete book in post first"));
        } else {
            book.setDeleted(true);
            bookRepository.save(book);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Delete Successfully"));
    }

    public BookDTO convertToDTO (Book book) {
        if (book == null) {
            return null;
        }
        BookDTO bookDTO = modelMapper.map(book, BookDTO.class);

        // map postId
        if(book.getPost() == null) {
            bookDTO.setPostId(null);
        }else {
            bookDTO.setPostId(book.getPost().getId());
        }
        // map author
        Set<String> authorNames = new HashSet<>();
        Set<Author> authors = book.getAuthors();
        for (Author author: authors
             ) {
            authorNames.add(author.getName());
        }
        bookDTO.setAuthors(authorNames);
        // map image
        Set<String> images = book.getBookImages().stream().map(BookImage::getImage).collect(Collectors.toSet());
        bookDTO.setImage(images);
        // map createBy
        bookDTO.setCreateBy(book.getCreateBy().getEmail());
        // map updateBy
        bookDTO.setUpdateBy(book.getUpdateBy());
        // map mainCategory
        bookDTO.setMainCategory(book.getMainCategory().getName());
        // map subCategory
        if(book.getSubCategory() == null) {
            bookDTO.setSubCategory(null);
        }else {
            bookDTO.setSubCategory(book.getSubCategory());
        }
        // map subSubCategory
        if(book.getSubSubCategory() == null) {
            bookDTO.setSubSubCategory(null);
        } else {
            bookDTO.setSubSubCategory(book.getSubSubCategory());
        }

        return bookDTO;
    }
}
