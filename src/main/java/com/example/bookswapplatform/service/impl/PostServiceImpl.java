package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.common.ExchangeMethod;
import com.example.bookswapplatform.common.FilterRequest;
import com.example.bookswapplatform.common.PageableRequest;
import com.example.bookswapplatform.common.Pagination;
import com.example.bookswapplatform.dto.*;
import com.example.bookswapplatform.entity.Area.District;
import com.example.bookswapplatform.entity.Book.Author;
import com.example.bookswapplatform.entity.Book.Book;
import com.example.bookswapplatform.entity.Post.Post;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.*;
import com.example.bookswapplatform.service.PostService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.Condition;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final PostStatusRepository postStatusRepository;
    private final BookRepository bookRepository;
    private final BookServiceImpl bookService;
    private final ModelMapper modelMapper;
    private final AreaRepository areaRepository;
    private final DistrictRepository districtRepository;
    private final AreaServiceImpl areaService;
    @Override
    public ResponseEntity<BaseResponseDTO> createPost(Principal principal, PostRequest postRequest) {
        Set<Book> books = new HashSet<>();

        Post post = modelMapper.map(postRequest, Post.class);
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        post.setCreateBy(user);
        post.setPostStatus(postStatusRepository.findByName("ACTIVE")
                .orElseThrow(()->new ResourceNotFoundException("Post status: ACTIVE Not Found!")));

        for (UUID bookId: postRequest.getBookIds()
             ) {
            Book book = new Book();
            book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book with id:" + bookId + "Not Found!"));
            // set book price if Trade, give price = 0
            if(postRequest.getExchangeMethod().equals("TRADE") || postRequest.getExchangeMethod().equals("GIVE") ) {
                book.setPrice(BigDecimal.valueOf(0));
            } else {
                for (BigDecimal price: postRequest.getPrice()
                     ) {
                    book.setPrice(price);
                }
            }
            if(book.getPost() != null) {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Book already in other post"));
            }
            if(book.isDone()) {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Book already done"));
            }
            books.add(book);

        }

        // set book to post
        post.setBooks(books);

        post.setArea(areaRepository.findByCity(postRequest.getCity())
                .orElseThrow(() -> new ResourceNotFoundException("City:" + postRequest.getCity() + "Not Found!")));
        post.setDistrict(districtRepository.findByDistrict(postRequest.getDistrict())
                .orElseThrow(() -> new ResourceNotFoundException("District:" + postRequest.getDistrict() + "Not Found!")));



        postRepository.save(post);
        for (Book book: books
        ) {
            book.setPost(post);
            bookRepository.save(book);
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.CREATED, "Create Successfully"));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getUserPost(Principal principal) {
        List<PostDTO> postDTOS = new ArrayList<>();
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(()->new ResourceNotFoundException("User Not Found"));
        List<Post> posts = postRepository.findByCreateBy(user);
        if(posts.isEmpty()) {
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully", null,null));
        }
        for (Post post: posts
             ) {
            PostDTO postDTO = convertToDTO(post);
            postDTOS.add(postDTO);
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully", null, postDTOS));
    }


    @Override
    public ResponseEntity<BaseResponseDTO> filterPost(int pageNumber,
                                                      int pageSize,
                                                      String sortBy,
                                                      String sortOrder,
                                                      String keyWord,
                                                      FilterRequest filterRequest) {

        PageableRequest pageableRequest = new PageableRequest(pageNumber, pageSize, sortBy, sortOrder);
        Pageable pageable = pageableRequest.toPageable();
        Page<Post> postPage;

        // if key word is null will search all post
        if(keyWord == null || keyWord.isEmpty()) {
            postPage = postRepository.findAllNotDeactive(pageable);
        } else {
            postPage = postRepository.searchPostsByKeyword(keyWord, pageable);
        }
        List<Post> posts = postPage.getContent();
        List<PostDTO> postDTOS = new ArrayList<>();
        if (filterRequest == null || (
                filterRequest.getCity() == null &&
                        filterRequest.getCategory() == null &&
                        filterRequest.getSubCategory() == null &&
                        filterRequest.getSubSubCategory() == null &&
                        filterRequest.getCreateDate() == null &&
                        filterRequest.getPublishedDate() == null &&
                        filterRequest.getPublisher() == null &&
                        filterRequest.getAuthors() == null  &&
                        filterRequest.getDistrict() == null &&
                        filterRequest.getExchangeMethod() == null &&
                        filterRequest.getLanguage() == null)) {
            postDTOS = posts.stream().map(this::convertToDTO).toList();

        } else {
            postDTOS = posts.stream()
                    .filter(post -> matchesFilter(post, filterRequest))
                    .map(this::convertToDTO)
                    .toList();
        }
        Pagination pagination = new Pagination(postPage.getNumber(), postPage.getTotalElements(), postPage.getTotalPages());
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", pagination, postDTOS));
    }
private boolean matchesFilter(Post post, FilterRequest filterRequest) {
    // Apply filter conditions to the post and its book properties.
    Set<Author> authors = post.getBooks()
            .stream()
            .flatMap(book -> book.getAuthors().stream())
            .collect(Collectors.toSet());

    if(filterRequest.getCategory() != null && (filterRequest.getSubCategory() != null || filterRequest.getSubSubCategory() != null)) {
        return filterBookSetWhenHaveTwoCategory(post.getBooks(), filterRequest) &&
                filterPostAttributes(post, filterRequest) &&
                (filterRequest.getAuthors() == null || authors.stream().anyMatch(author -> author.getName().equals(filterRequest.getAuthors())));
    }else {
        return filterBookSet(post.getBooks(), filterRequest) &&
                filterPostAttributes(post, filterRequest) &&
                (filterRequest.getAuthors() == null || authors.stream().anyMatch(author -> author.getName().equals(filterRequest.getAuthors())));
    }
}

    private boolean filterBookSetWhenHaveTwoCategory(Set<Book> bookSet, FilterRequest filterRequest) {
        return bookSet.stream().anyMatch(book ->
                        (filterRequest.getCategory() == null || book.getMainCategory().getName().equals(filterRequest.getCategory())) ||
                                ((filterRequest.getSubCategory() == null || Objects.equals(book.getSubCategory(), filterRequest.getSubCategory())) ||
                                (filterRequest.getSubSubCategory() == null || Objects.equals(book.getSubSubCategory(), filterRequest.getSubSubCategory()))) &&
                                (filterRequest.getPublisher() == null || book.getPublisher().equals(filterRequest.getPublisher())) &&
//                                (filterRequest.getPublishedDate() == null || book.getPublishedDate().equals(DateTimeUtils.convertStringToLocalDate(filterRequest.getPublishedDate()))) &&
                                (filterRequest.getLanguage() == null || book.getLanguage().toString().equals(filterRequest.getLanguage()))

        );
    }

    private boolean filterBookSet(Set<Book> bookSet, FilterRequest filterRequest) {
        return bookSet.stream().anyMatch(book ->
                        (filterRequest.getCategory() == null || book.getMainCategory().getName().equals(filterRequest.getCategory())) &&
                                (filterRequest.getSubCategory() == null || Objects.equals(book.getSubCategory(), filterRequest.getSubCategory())) &&
                                (filterRequest.getSubSubCategory() == null || Objects.equals(book.getSubSubCategory(), filterRequest.getSubSubCategory())) &&
                                (filterRequest.getPublisher() == null || book.getPublisher().equals(filterRequest.getPublisher())) &&
//                                (filterRequest.getPublishedDate() == null || book.getPublishedDate().equals(DateTimeUtils.convertStringToLocalDate(filterRequest.getPublishedDate()))) &&
                                (filterRequest.getLanguage() == null || book.getLanguage().toString().equals(filterRequest.getLanguage()))

        );
    }

    private boolean filterPostAttributes(Post post, FilterRequest filterRequest){
        return //(filterRequest.getCreateDate() == null || post.getCreateDate().equals(DateTimeUtils.convertStringToLocalDateTime(filterRequest.getCreateDate()))) &&
                (filterRequest.getCity() == null || post.getArea().getCity().equals(filterRequest.getCity())) &&
                (filterRequest.getDistrict() == null || post.getDistrict().getDistrict().equals(filterRequest.getDistrict())) &&
                (filterRequest.getExchangeMethod() == null || post.getExchangeMethod().toString().equals(filterRequest.getExchangeMethod()));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getPostDetail(UUID id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post with id:" + id + "Not Found!"));
        PostDTO postDTO = convertToDTO(post);
        int views = post.getViews() + 1;
        post.setViews(views);
        postRepository.save(post);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, postDTO));

    }

    @Override
    public ResponseEntity<BaseResponseDTO> modifyPost(Principal principal, UUID postId, PostUpdateRequest postUpdateRequest) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post with id:" + postId + "Not Found!"));
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(()->new ResourceNotFoundException("User Not Found"));
        post.setUpdateBy(user.getEmail());
        Condition<?, ?> skipNullAndSameValue = ctx ->
                ctx.getSource() != null && !ctx.getSource().equals(ctx.getDestination());

        modelMapper.typeMap(PostUpdateRequest.class, Post.class)
                .addMappings(mapper -> {
                    mapper.when(skipNullAndSameValue).map(PostUpdateRequest::getCaption, Post::setCaption);
                    mapper.when(skipNullAndSameValue).map(PostUpdateRequest::getDescription, Post::setDescription);
                    mapper.skip(Post::setArea);
                    mapper.skip(Post::setDistrict);
                    mapper.skip(Post::setExchangeMethod);
                    mapper.skip(Post::setBooks);

                });
        modelMapper.map(postUpdateRequest, post);
        if(postUpdateRequest.getCity() != null && !postUpdateRequest.getCity().equals(post.getArea().getCity())) {
            post.setArea(areaRepository.findByCity(postUpdateRequest.getCity())
                    .orElseThrow(() -> new ResourceNotFoundException("City:" + postUpdateRequest.getCity() + "Not Found!")));
        }
        if(postUpdateRequest.getDistrict() != null && !postUpdateRequest.getDistrict().equals(post.getDistrict().getDistrict())) {
            for (District district: post.getArea().getDistricts()
                 ) {
                if(postUpdateRequest.getDistrict().equals(district.getDistrict())) {
                    post.setDistrict(districtRepository.findByDistrict(postUpdateRequest.getDistrict())
                            .orElseThrow(() -> new ResourceNotFoundException("District:" + postUpdateRequest.getDistrict() + "Not Found!")));
                } else {
                    return ResponseEntity.badRequest()
                            .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "District is not in city"));
                }
            }

        }

        if(postUpdateRequest.getExchangeMethod() != null &&
                !ExchangeMethod.valueOf(postUpdateRequest.getExchangeMethod()).equals(post.getExchangeMethod())) {
            Set<Book> books = post.getBooks();
            for (Book book: books
            ) {
                if(postUpdateRequest.getExchangeMethod().equals("TRADE") || postUpdateRequest.getExchangeMethod().equals("GIVE") ) {
                    book.setPrice(BigDecimal.valueOf(0));
                    if(postUpdateRequest.getExchangeMethod().equals("TRADE")) {
                        post.setExchangeMethod(ExchangeMethod.TRADE);
                    } else {
                        post.setExchangeMethod(ExchangeMethod.GIVE);
                    }
                } else {
                    for (BigDecimal price: postUpdateRequest.getPrice()
                    ) {
                        for (UUID id: postUpdateRequest.getBookPriceId()
                             ) {
                            if(book == bookRepository.findById(id)
                                    .orElseThrow(() -> new ResourceNotFoundException("Book with id:" + postUpdateRequest.getBookPriceId() + "Not Found!"))) {
                                book.setPrice(price);

                            }
                        }
                    }
                    post.setExchangeMethod(ExchangeMethod.SELL);
                }
                bookRepository.save(book);
            }
        }
        postRepository.save(post);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Modify Successfully"));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> modifyBookInPost(Principal principal, UUID postId, PostUpdateBookRequest postUpdateBookRequest) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post with id:" + postId + "Not Found!"));
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(()->new ResourceNotFoundException("User Not Found"));
        post.setUpdateBy(user.getEmail());
        //update sách bằng cách thêm sách vào bài đăng
        if (postUpdateBookRequest.getOldBookId() == null &&
                postUpdateBookRequest.getNewBookId() != null &&
                postUpdateBookRequest.getUpdateMethod().equals("Add")) {
            Book newBook = bookRepository.findById(postUpdateBookRequest.getNewBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book:" + postUpdateBookRequest.getOldBookId() + "Not Found!"));
            if (!checkBookInPost(post.getBooks(), newBook)) {
                Set<Book> books = new HashSet<>();
                books.add(newBook);
                newBook.setPost(post);
                bookRepository.save(newBook);
                post.setBooks(books);
                postRepository.save(post);
                return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Modify Successfully"));
            }
        }

        // update sách bằng cách gỡ sách ra khỏi bài đăng
        if(postUpdateBookRequest.getOldBookId() != null &&
                postUpdateBookRequest.getNewBookId() == null &&
                postUpdateBookRequest.getUpdateMethod().equals("Delete")) {
            Book oldBook = bookRepository.findById(postUpdateBookRequest.getOldBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Book:" + postUpdateBookRequest.getOldBookId() + "Not Found!"));
            if (checkBookInPost(post.getBooks(), oldBook)) {
                oldBook.setPost(null);
                oldBook.setPrice(BigDecimal.valueOf(0));
                bookRepository.save(oldBook);
                postRepository.save(post);
                return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Modify Successfully"));
            } else {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "This is not exist book in post"));
            }
        }
        return ResponseEntity.badRequest()
                .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Error"));
    }

    public boolean checkBookInPost (Set<Book> books,Book bookCheck) {
        for (Book book: books
             ) {
            if(book == bookCheck) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResponseEntity<BaseResponseDTO> deletePost(UUID postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post with id:" + postId + "Not Found!"));
        post.setDeleted(true);
        for (Book book: post.getBooks()
             ) {
            book.setPost(null);
            book.setPrice(BigDecimal.valueOf(0));
            bookRepository.save(book);
        }
        postRepository.save(post);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Delete Successfully"));
    }

    public PostDTO convertToDTO (Post post) {
        Set<BookDTO> bookDTOS = new HashSet<>();
        if(post == null) {
            return null;
        }
        PostDTO postDTO = modelMapper.map(post, PostDTO.class);
        postDTO.setCreateBy(post.getCreateBy().getEmail());
        postDTO.setUpdateBy(post.getUpdateBy());
        postDTO.setCity(post.getArea().getCity());
        postDTO.setDistrict(post.getDistrict().getDistrict());
        postDTO.setPostStatus(post.getPostStatus().getName());
        for (Book book: post.getBooks()
             ) {
            BookDTO bookDTO = bookService.convertToDTO(book);
            bookDTOS.add(bookDTO);
        }
        postDTO.setBookDTOS(bookDTOS);
        return postDTO;
    }
}
