package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.dto.BaseResponseDTO;
import com.example.bookswapplatform.dto.PaymentDTO;
import com.example.bookswapplatform.entity.Book.Book;
import com.example.bookswapplatform.entity.Order.OrderStatus;
import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Payment.Payment;
import com.example.bookswapplatform.entity.Payment.Status;
import com.example.bookswapplatform.entity.Payment.Transaction.Transaction;
import com.example.bookswapplatform.entity.Payment.UserWallet;
import com.example.bookswapplatform.entity.Post.Post;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.*;
import com.example.bookswapplatform.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final PostStatusRepository postStatusRepository;
    private final BookRepository bookRepository;
    private final PostRepository postRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<BaseResponseDTO> checkoutForUserRequest(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found!"));
        for (Transaction transaction: payment.getTransactions()
             ) {
            UserWallet wallet = transaction.getToWallet();

            if(wallet.getBalance().compareTo(transaction.getAmount().add(transaction.getFee())) < 0) {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Not have enough balance"));

            }
            BigDecimal newBalance = wallet.getBalance().subtract(transaction.getAmount().add(transaction.getFee()));
            wallet.setBalance(newBalance);
            transaction.setStatus("SUCCESS");

        }
        payment.setStatus(Status.SUCCESS);
        Orders orders = payment.getOrders();
        orders.setPayment(true);
        orders.setOrderStatus(OrderStatus.PAID);
        orderRepository.save(orders);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully"));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> checkoutForUserGetRequest(UUID paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found!"));
        for (Transaction transaction: payment.getTransactions()
        ) {
            UserWallet wallet = transaction.getToWallet();

            if(wallet.getBalance().compareTo(transaction.getAmount().add(transaction.getFee())) < 0) {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Not have enough balance"));

            }
            BigDecimal newBalance = wallet.getBalance().subtract(transaction.getAmount().add(transaction.getFee()));
            wallet.setBalance(newBalance);
            transaction.setStatus("SUCCESS");

        }
        payment.setStatus(Status.SUCCESS);
        Orders orders = payment.getOrders();
        orders.setConfirm(true);
        //xu ly ben post va book
        List<Book> books = orderDetailRepository.findBooksInOrder(orders);
        for (Book book: books
        ) {
            book.setDone(true);
            bookRepository.save(book);

        }
        orderRepository.save(orders);
        //check neu tat ca book trong post da hoan thanh thi doi trang thai post
        Set<Book> bookSet = orders.getPost().getBooks();
        Post post = orders.getPost();
        if(bookSet.stream().allMatch(Book::isDone)) {
            post.setPostStatus(postStatusRepository.findByName("DEACTIVE")
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found!")));
            postRepository.save(post);
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully"));
    }

    public PaymentDTO convertToDTO (Payment payment) {
        if(payment == null ) {
            return null;
        }
        return modelMapper.map(payment, PaymentDTO.class);
    }

}
