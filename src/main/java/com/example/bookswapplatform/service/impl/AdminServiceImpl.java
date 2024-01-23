package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.common.FilterRequest;
import com.example.bookswapplatform.common.PageableRequest;
import com.example.bookswapplatform.common.Pagination;
import com.example.bookswapplatform.dto.*;
import com.example.bookswapplatform.entity.Book.Author;
import com.example.bookswapplatform.entity.Book.Book;
import com.example.bookswapplatform.entity.Book.MainCategory;
import com.example.bookswapplatform.entity.Order.OrderShipping;
import com.example.bookswapplatform.entity.Order.OrderStatus;
import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Post.Post;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.*;
import com.example.bookswapplatform.service.AdminService;
import com.example.bookswapplatform.service.PostServiceHelper;
import com.example.bookswapplatform.utils.DateTimeUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {
    private final OrderServiceImpl orderService;
    private final UserServiceImpl userService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PostRepository postRepository;
    private final CategoryRepository categoryRepository;
    private final BookRepository bookRepository;
    private final PostServiceHelper postServiceHelper;
    @Override
    public ResponseEntity<BaseResponseDTO> orderFilter(int pageNumber, int pageSize, String sortBy, String sortOrder, String keyWord, String status) {
        PageableRequest pageableRequest = new PageableRequest(pageNumber, pageSize, sortBy, sortOrder);
        Pageable pageable = pageableRequest.toPageable();
        Page<Orders> ordersPage = null;
        List<Orders> ordersList;
        List<OrderDTO> orderDTOS = new ArrayList<>();

        if (keyWord == null && status == null) {
            ordersPage = orderRepository.findAll(pageable);
        } else if (keyWord != null && status == null) {
            ordersPage = orderRepository.searchByKeyWord(keyWord, pageable);
        } else if (keyWord == null && status != null) {
            ordersPage = orderRepository.searchByStatus(OrderStatus.valueOf(status), pageable);
        }

        if (ordersPage != null) {
            ordersList = ordersPage.getContent();
            orderDTOS = convertToOrderDTO(ordersList);
            Pagination pagination = new Pagination(ordersPage.getNumber(), ordersPage.getTotalElements(), ordersPage.getTotalPages());
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", pagination, orderDTOS));
        } else {
            // Handle the case when ordersPage is null (possibly return an error response)
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, null));
        }
    }


    public List<OrderDTO> convertToOrderDTO(List<Orders> orders) {
        List<OrderDTO> orderDTOS = new ArrayList<>();
        for (Orders order: orders) {
            orderDTOS.add(orderService.convertToDTO(order));
        }
        return orderDTOS;
    }


    @Override
    public ResponseEntity<BaseResponseDTO> userFilter(int pageNumber, int pageSize, String sortBy, String sortOrder, String keyWord, String role) {
        PageableRequest pageableRequest = new PageableRequest(pageNumber, pageSize, sortBy, sortOrder);
        Pageable pageable = pageableRequest.toPageable();
        Page<User> userPage = null;
        List<User> userList;
        List<UserDTO> userDTOS = new ArrayList<>();
        if (keyWord == null && role == null) {
            userPage = userRepository.findAll(pageable);
        } else if (keyWord != null && role == null) {
            userPage = userRepository.searchByKeyWord(keyWord, pageable);
        } else if (keyWord == null && role != null) {
            userPage = userRepository.searchByRole(role, pageable);
        }
        if (userPage != null) {
            userList = userPage.getContent();
            userDTOS = convertToUserDTO(userList);
            Pagination pagination = new Pagination(userPage.getNumber(), userPage.getTotalElements(), userPage.getTotalPages());
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", pagination, userDTOS));
        } else {
            // Handle the case when ordersPage is null (possibly return an error response)
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, null));
        }
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getDataWithinDateRange(int days, String object) {
        List<StatisticalDTO> statisticalDTOList = new ArrayList<>();
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);
        if(object.equals("POST")) {
            List<Post> postList = postRepository.findByCreateDateBetween(startDate, endDate);
            List<Post> postsWithinRange = postList.stream()
                    .filter(post -> !post.getCreateDate().isBefore(startDate) && !post.getCreateDate().isAfter(endDate))
                    .collect(Collectors.toList());
            statisticalDTOList = convertToStatisticalDTO(postsWithinRange, Post::getCreateDate);
        }
        if (object.equals("BOOK")) {
            List<Book> bookList = bookRepository.findByCreateDateBetween(startDate, endDate);
            List<Book> booksWithinRange = bookList.stream()
                    .filter(book -> !book.getCreateDate().isBefore(startDate) && !book.getCreateDate().isAfter(endDate))
                    .collect(Collectors.toList());
            statisticalDTOList = convertToStatisticalDTO(booksWithinRange, Book::getCreateDate);
        }
        if (object.equals("ORDERS")) {
            List<Orders> ordersList = orderRepository.findByCreateDateBetween(startDate, endDate);
            List<Orders> ordersWithinRange = ordersList.stream()
                    .filter(orders -> !orders.getCreateDate().isBefore(startDate) && !orders.getCreateDate().isAfter(endDate))
                    .collect(Collectors.toList());
            statisticalDTOList = convertToStatisticalDTO(ordersWithinRange, Orders::getCreateDate);
        }
        if (object.equals("USER")) {
            List<User> userList = userRepository.findByCreateDateBetween(startDate,endDate);
            List<User> userWithinRange = userList.stream()
                    .filter(user -> !user.getCreateDate().isBefore(startDate) && !user.getCreateDate().isAfter(endDate))
                    .collect(Collectors.toList());
            statisticalDTOList = convertToStatisticalDTO(userWithinRange, User::getCreateDate);
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, statisticalDTOList));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getObjectCount(String object) {
        long number = 0;
        if(object.equals("POST")){
            number = postRepository.count();
        }
        if(object.equals("BOOK")){
            number = bookRepository.count();

        }
        if(object.equals("ORDERS")){
            number = orderRepository.count();

        }
        if(object.equals("USER")){
            number = userRepository.count();

        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, number));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> revenueStatistic(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        List<Orders> ordersList = orderRepository.findByOrderStatusAndUpdateDateBetween(OrderStatus.FINISH, startDate, endDate);

        Map<LocalDate, BigDecimal> revenueByDay = new HashMap<>();

        for (Orders order : ordersList) {
            BigDecimal fee = order.getFee();
            BigDecimal shipPrice = order.getShipPrice();
            String percentPay = order.getShippingOrders()
                    .stream()
                    .findFirst()
                    .map(OrderShipping::getPercentPay)
                    .orElse(null);
            assert percentPay != null;
            BigDecimal percentPay1 = new BigDecimal(percentPay)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal shipPriceAfterGiveShipper = shipPrice.subtract(shipPrice.multiply(percentPay1));

            LocalDateTime orderDay = order.getUpdateDate().toLocalDate().atStartOfDay();

            revenueByDay.merge(orderDay.toLocalDate(), fee.add(shipPriceAfterGiveShipper), BigDecimal::add);
        }

        List<RevenueStatisticDTO> statisticalDTOList = revenueByDay.entrySet().stream()
                .map(entry -> new RevenueStatisticDTO(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, statisticalDTOList));
    }

    @Override
    @Transactional
    public ResponseEntity<BaseResponseDTO> addCategory(CategoryRequest categoryRequest) {
        try {
            if (categoryRequest.getCategory() == null) {
                return ResponseEntity.badRequest().body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Category name cannot be empty", null, null));
            }

            MainCategory mainCategory = new MainCategory();
            mainCategory.setName(categoryRequest.getCategory());
            categoryRepository.save(mainCategory);

            if (categoryRequest.getSubCategory() != null && !categoryRequest.getSubCategory().isEmpty()) {
                for (String subCategoryName : categoryRequest.getSubCategory()) {
                    MainCategory subCategory = new MainCategory();
                    subCategory.setName(subCategoryName);
                    subCategory.setParentCategory(mainCategory);
                    categoryRepository.save(subCategory);
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred", null, e.getMessage()));
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, null));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> modifyPostCreateDate(UUID id,String createDate, Set<String> categories) throws ParseException {
        Post post = postRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Post Not Found"));
        if (createDate != null) {
            post.setCreateDate(DateTimeUtils.convertStringToLocalDateTime(createDate));
        }
        if (categories != null) {
            post.setCategories(categories);
        }
        postRepository.save(post);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, null));
    }


    @Override
    @Transactional
    public ResponseEntity<BaseResponseDTO> addSubSubCategory(UUID id, Set<String> subSubCategoryNames) {
        try {
            if (id == null) {
                return ResponseEntity.badRequest().body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Category name cannot be empty", null, null));
            }
            MainCategory subCategory = categoryRepository.findById(id)
                    .orElseThrow(()->new ResourceNotFoundException("Category Not Found"));
            if (subSubCategoryNames != null && !subSubCategoryNames.isEmpty()) {
                for (String subSubCategoryName : subSubCategoryNames) {
                    MainCategory subSubCategory = new MainCategory();
                    subSubCategory.setName(subSubCategoryName);
                    subSubCategory.setParentCategory(subCategory);
                    categoryRepository.save(subSubCategory);
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR, "Error occurred", null, e.getMessage()));
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, null));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> postFilter(int pageNumber, int pageSize, String sortBy, String sortOrder, String keyWord, String status, FilterRequest filterRequest) {
        PageableRequest pageableRequest = new PageableRequest(pageNumber, pageSize, sortBy, sortOrder);
        Pageable pageable = pageableRequest.toPageable();
        Page<Post> postPage;

        // if key word is null will search all post
        if(keyWord == null || keyWord.isEmpty()) {
            postPage = postRepository.findAll(pageable);
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
            postDTOS = posts.stream().map(postServiceHelper::convertToDTO).toList();

        } else {
            postDTOS = posts.stream()
                    .filter(post -> matchesFilter(post, filterRequest))
                    .map(postServiceHelper::convertToDTO)
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

        if (filterRequest.getCategory() != null && (filterRequest.getSubCategory() != null || filterRequest.getSubSubCategory() != null)) {
            return filterBookSetWhenHaveTwoCategory(post.getBooks(), filterRequest) &&
                    filterPostAttributes(post, filterRequest) &&
                    (filterRequest.getAuthors() == null || authors.stream().anyMatch(author -> author.getName().equals(filterRequest.getAuthors())));
        } else {
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

    private boolean filterPostAttributes(Post post, FilterRequest filterRequest) {
        return //(filterRequest.getCreateDate() == null || post.getCreateDate().equals(DateTimeUtils.convertStringToLocalDateTime(filterRequest.getCreateDate()))) &&
                (filterRequest.getCity() == null || post.getArea().getCity().equals(filterRequest.getCity())) &&
                        (filterRequest.getDistrict() == null || post.getDistrict().getDistrict().equals(filterRequest.getDistrict())) &&
                        (filterRequest.getExchangeMethod() == null || post.getExchangeMethod().toString().equals(filterRequest.getExchangeMethod()));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> modifyCategory(CategoryRequest categoryRequest) {
        return null;
    }

    public <T> List<StatisticalDTO> convertToStatisticalDTO(List<T> objectsList, Function<T, LocalDateTime> getCreateDateFunction) {
        return objectsList.stream()
                .collect(Collectors.groupingBy(object -> getCreateDateFunction.apply(object).toLocalDate()))
                .entrySet()
                .stream()
                .map(entry -> new StatisticalDTO(entry.getKey(), entry.getValue().size()))
                .collect(Collectors.toList());
    }


    public List<UserDTO> convertToUserDTO (List<User> userList) {
        List<UserDTO> userDTOS = new ArrayList<>();
        for (User user: userList
             ) {
            userDTOS.add(userService.convertToDTO(user));
        }
        return userDTOS;
    }
}
