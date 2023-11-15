package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.dto.*;
import com.example.bookswapplatform.entity.Book.Book;
import com.example.bookswapplatform.entity.Order.OrderDetail;
import com.example.bookswapplatform.entity.Order.OrderStatus;
import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Payment.Payment;
import com.example.bookswapplatform.entity.Payment.Status;
import com.example.bookswapplatform.entity.Payment.Transaction.Transaction;
import com.example.bookswapplatform.entity.Payment.UserWallet;
import com.example.bookswapplatform.entity.Post.Post;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.*;
import com.example.bookswapplatform.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final PostRepository postRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final PostStatusRepository postStatusRepository;
    private final PostServiceImpl postService;
    private final BookServiceImpl bookService;
    private final OrderDetailRepository orderDetailRepository;
    private final UserWalletRepository userWalletRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentServiceImpl paymentService;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<BaseResponseDTO> createOrder(Principal principal, UUID postId, List<UUID> bookIds) {
        Orders orders = new Orders();
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post with id:" + postId + "Not Found!"));
        //check post available
        if (post.getPostStatus().getName().equals("LOCKED") || post.getPostStatus().getName().equals("DEACTIVE")) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Post had been locked"));
        }

        BigDecimal value = new BigDecimal(0);
        orders.setCreateBy(user);
        orders.setPost(post);
        orders.setPrice(value);
        orders.setNote(null);
        orders.setArea(post.getArea());
        orders.setStarShipDate(null);
        orders.setFinishShipDate(null);
        orders.setUpdateBy(user.getEmail());
        orders.setOrderStatus(OrderStatus.NOT_PAY);
        orders.setDistrict(post.getDistrict());
        orders.setConfirm(false);
        orders.setPayment(false);
        orders.setOrderDetails(null);
        orders.setPayments(null);

        orderRepository.save(orders);

        createPaymentForUserRequest(orders);

        Set<Book> books = new HashSet<>();
        Set<OrderDetail> orderDetails = new HashSet<>();
        for (UUID bookId : bookIds
        ) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book with id:" + bookId + "Not Found!"));

            if(book.isDone()) {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Book not available"));
            }
            books.add(book);
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setBook(book);
            orderDetail.setOrders(orders);
            orderDetails.add(orderDetail);
            //check order confirm
            if(orderDetailRepository.findConfirmedOrderDetailsForBooks(book) == null) {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Book not available"));

            }
        }

        orderDetailRepository.saveAll(orderDetails);

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.CREATED, "Order id: " + orders.getId()));
    }

    public void createPaymentForUserRequest(Orders orders) {
        Payment payment = new Payment();
        //Set<Payment> payments = new HashSet<>();
        payment.setAmount(orders.getPrice());
        BigDecimal value = new BigDecimal(2000);
        payment.setFee(value);
        payment.setStatus(Status.ON_GOING);
        payment.setOrders(orders);
        payment.setCreateBy(orders.getCreateBy().getEmail());

        Transaction transaction = new Transaction();
        Set<Transaction> transactions = new HashSet<>();
        transaction.setAmount(orders.getPrice());
        transaction.setFee(payment.getFee());
        transaction.setCreateBy(payment.getCreateBy());
        UserWallet userWallet = userWalletRepository.getUserWalletByCreateBy(orders.getCreateBy())
                .orElseThrow(() -> new ResourceNotFoundException("User wallet not found!"));
        transaction.setToWallet(userWallet);

        transactions.add(transaction);

        payment.setTransactions(transactions);
        paymentRepository.save(payment);

        //payments.add(payment);
        //orders.setPayments(payments);
        //orderRepository.save(orders);

        transaction.setPayment(payment);
        transactionRepository.save(transaction);
    }

    public Payment createPaymentForUserGetRequest(Orders orders) {
        Payment payment = new Payment();
        payment.setAmount(BigDecimal.valueOf(0));
        payment.setFee(BigDecimal.valueOf(2000));
        payment.setStatus(Status.ON_GOING);
        payment.setOrders(orders);
        payment.setCreateBy(orders.getPost().getCreateBy().getEmail());
        paymentRepository.save(payment);

        Transaction transaction = new Transaction();
        transaction.setAmount(BigDecimal.valueOf(0));
        transaction.setFee(payment.getFee());
        transaction.setCreateBy(payment.getCreateBy());
        UserWallet userWallet = userWalletRepository.getUserWalletByCreateBy(orders.getPost().getCreateBy())
                .orElseThrow(() -> new ResourceNotFoundException("User wallet not found!"));
        transaction.setToWallet(userWallet);
        transaction.setPayment(payment);
        transactionRepository.save(transaction);

        return payment;
    }


    @Override
    public ResponseEntity<BaseResponseDTO> getOrderDetailsById(UUID id) {
        return null;
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllRequestUserOrders(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersList = orderRepository.findByCreateBy(user);

        Set<OrderDTO> orderDTOS = new HashSet<>();
        for (Orders orders: ordersList
             ) {
            OrderDTO orderDTO = convertToDTO(orders);
            orderDTOS.add(orderDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getRequestUserOrdersNotPay(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<OrderDTO> orderDTOSet = new HashSet<>();
        Set<Orders> ordersList = orderRepository.findOrdersByCreateByAndPayment(user);

        for (Orders orders: ordersList
        ) {
            OrderDTO orderDTO = convertToDTO(orders);
            orderDTOSet.add(orderDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOSet));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllUserOrdersNotConfirm(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersSet = orderRepository.findOrdersWithBooksCreatedByUser(user);
        Set<OrderDTO> orderDTOS = new HashSet<>();
        ordersSet.forEach(orders -> {
            OrderDTO orderDTO = convertToDTO(orders);
            orderDTOS.add(orderDTO);
        });

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> confirmOrder(UUID orderId) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found!"));
        //check orders is paid or not
        if(orders.isPayment() && !orders.isConfirm()) {
            Set<Payment> payments = new HashSet<>();
            payments.add(createPaymentForUserGetRequest(orders));
            orders.setPayments(payments);
            orderRepository.save(orders);

        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully"));
    }

    public OrderDTO convertToDTO (Orders orders) {
        if(orders == null) {
            return null;
        }
        Set<BookDTO> bookDTOS = new HashSet<>();
        OrderDTO orderDTO = modelMapper.map(orders, OrderDTO.class);
        orderDTO.setCreateBy(orders.getCreateBy().getEmail());
        orderDTO.setUpdateBy(orders.getUpdateBy());
        orderDTO.setCity(orders.getArea().getCity());
        orderDTO.setDistrict(orders.getDistrict().getDistrict());
        PostDTO postDTO = postService.convertToDTO(orders.getPost());
        orderDTO.setPostDTO(postDTO);
        for (Book book: orderDetailRepository.findBooksInOrder(orders)
        ) {
            BookDTO bookDTO = bookService.convertToDTO(book);
            bookDTOS.add(bookDTO);
        }
        orderDTO.setBookTradeDTOS(bookDTOS);

        Set<PaymentDTO> paymentDTOS = new HashSet<>();
        for (Payment payment : orders.getPayments()) {
            PaymentDTO paymentDTO = paymentService.convertToDTO(payment);
            paymentDTOS.add(paymentDTO);
        }
        orderDTO.setPaymentDTOS(paymentDTOS);

        return orderDTO;

    }
}

