package com.example.bookswapplatform.service.impl;


import com.example.bookswapplatform.dto.*;
import com.example.bookswapplatform.entity.Post.PostStatus;
import com.example.bookswapplatform.service.OrderService;
import com.example.bookswapplatform.entity.Book.Book;
import com.example.bookswapplatform.entity.Order.OrderDetail;
import com.example.bookswapplatform.entity.Order.OrderStatus;
import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Payment.Payment;
import com.example.bookswapplatform.entity.Payment.Status;
import com.example.bookswapplatform.entity.Post.Post;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.*;
import com.example.bookswapplatform.service.PostServiceHelper;
import com.example.bookswapplatform.utils.OrderScheduler;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final PostServiceHelper postServiceHelper;
    private final BookServiceImpl bookService;
    private final OrderDetailRepository orderDetailRepository;
    private final UserWalletRepository userWalletRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentServiceImpl paymentService;
    private final OrderScheduler orderScheduler;
    private final ModelMapper modelMapper;


    @Override
    public ResponseEntity<BaseResponseDTO> createOrder(Principal principal, UUID postId, OrderRequest orderRequest) {
        Orders orders = new Orders();
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post with id:" + postId + "Not Found!"));
        Set<Orders> ordersSet = orderRepository.findOrdersByCreateBy(user);
        for (Orders ordersCheck: ordersSet
             ) {
            if(ordersCheck.getPost() == post && !ordersCheck.getOrderStatus().equals(OrderStatus.CANCEL)) {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Already have order with this id"));
            }
        }
        if(post.getCreateBy() == user) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Can't create order with your own post"));
        }
        if(!orderRequest.isTradeOrderValid(post.getExchangeMethod())) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Have user book when exchange is sell or not have book trade when exchange is trade"));
        }
        //check post available
        if (post.getPostStatus().getName().equals("LOCKED") || post.getPostStatus().getName().equals("DEACTIVE")) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Post had been locked"));
        }

        BigDecimal bookPrice = new BigDecimal("0");
        user.setCity(orderRequest.getCity());
        user.setDistrict(orderRequest.getDistrict());
        userRepository.save(user);
        orders.setCreateBy(user);
        orders.setPost(post);
        orders.setNote(orderRequest.getNote());
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
        orders.setAutoRejectTime(LocalDateTime.now().plusHours(1));

        Set<OrderDetail> orderDetails = new HashSet<>();

        for (UUID bookId : orderRequest.getBookTradeIds()
        ) {
            Book book = bookRepository.findById(bookId)
                    .orElseThrow(() -> new ResourceNotFoundException("Book with id:" + bookId + "Not Found!"));

            if(book.isDone() || book.isLock()) {
                return ResponseEntity.badRequest()
                        .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Book not available"));
            }
            //set book của user tạo đơn khi trade là lock
            if(user.getBookList().contains(book)) {
                book.setLock(true);
            }

            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setBook(book);
            orderDetail.setOrders(orders);
            orderDetail.setPrice(book.getPrice());
            orderDetails.add(orderDetail);

            bookPrice = bookPrice.add(book.getPrice());
        }
        processOrderPrice(orderRequest, orders, bookPrice);
        orderRepository.save(orders);

        createPaymentForUserRequest(orders);

        orderDetailRepository.saveAll(orderDetails);
        orderScheduler.startOrderAutoRejectScheduler(orders);

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.CREATED, "Order id: " + orders.getId()));
    }

    public void processOrderPrice(OrderRequest orderRequest, Orders orders, BigDecimal bookPrice) {
        BigDecimal shipPrice = new BigDecimal("15000");
        BigDecimal fee = new BigDecimal("2000");

        orders.setBookPrice(bookPrice);
        orders.setFee(fee);

        if (orderRequest.getIsShip() == 0) {
            shipPrice = BigDecimal.ZERO;
            orders.setSenderShipPrice(shipPrice);
            orders.setReceiverShipPrice(shipPrice);
        } else {
            BigDecimal percentPay = new BigDecimal(orderRequest.getPercentPay())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            shipPrice = shipPrice.multiply(percentPay);
            orders.setSenderShipPrice(shipPrice);
            orders.setReceiverShipPrice(new BigDecimal("15000").subtract(shipPrice));
        }

        BigDecimal senderPrice = calculateSenderPrice(bookPrice, fee, orders.getSenderShipPrice());
        BigDecimal receiverPrice = calculateReceiverPrice(fee, orders.getReceiverShipPrice());

        orders.setSenderPrice(senderPrice);
        orders.setReceiverPrice(receiverPrice);
    }

    private BigDecimal calculateSenderPrice(BigDecimal bookPrice, BigDecimal fee, BigDecimal shipPrice) {
        return bookPrice.add(fee).add(shipPrice);
    }

    private BigDecimal calculateReceiverPrice(BigDecimal fee, BigDecimal shipPrice) {
        return shipPrice.add(fee);
    }

    public void createPaymentForUserRequest(Orders orders) {
        Payment payment = new Payment();
        //Set<Payment> payments = new HashSet<>();
        payment.setAmount(orders.getSenderPrice());
        BigDecimal value = new BigDecimal(2000);
        payment.setFee(value);
        payment.setStatus(Status.ON_GOING);
        payment.setOrders(orders);
        payment.setCreateBy(orders.getCreateBy().getEmail());

        paymentRepository.save(payment);

    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllRequestOrdersNotPayment(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersList = orderRepository.findByOrderStatusAndCreateBy(user, OrderStatus.NOT_PAY);

        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        for (Orders orders: ordersList
             ) {
            OrderGeneralDTO orderDTO = convertToOrderGeneralDTO(orders, orders.getSenderPrice());
            orderDTOS.add(orderDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllRequestOrdersWaitConfirm(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersList = orderRepository.findByOrderStatusAndCreateBy(user, OrderStatus.WAITING_CONFIRM);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        for (Orders orders: ordersList
        ) {
            OrderGeneralDTO orderDTO = convertToOrderGeneralDTO(orders, orders.getSenderPrice());
            orderDTOS.add(orderDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllRequestOrdersWaitShipper(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersList = orderRepository.findByOrderStatusAndCreateBy(user, OrderStatus.WAITING_SHIPPER);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        for (Orders orders: ordersList
        ) {
            OrderGeneralDTO orderDTO = convertToOrderGeneralDTO(orders, orders.getSenderPrice());
            orderDTOS.add(orderDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));

    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllRequestOrdersCancel(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersList = orderRepository.findByOrderStatusAndCreateBy(user, OrderStatus.CANCEL);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        for (Orders orders: ordersList
        ) {
            OrderGeneralDTO orderDTO = convertToOrderGeneralDTO(orders, orders.getSenderPrice());
            orderDTOS.add(orderDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllRequestOrdersFinish(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersList = orderRepository.findByOrderStatusAndCreateBy(user, OrderStatus.FINISH);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        for (Orders orders: ordersList
        ) {
            OrderGeneralDTO orderDTO = convertToOrderGeneralDTO(orders, orders.getSenderPrice());
            orderDTOS.add(orderDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllReceiveOrdersNeedConfirm(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersSet = orderRepository.findOrdersWithBooksCreatedByUser(user, OrderStatus.WAITING_CONFIRM);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        ordersSet.forEach(orders -> {
            OrderGeneralDTO orderGeneralDTO = convertToOrderGeneralDTO(orders, orders.getReceiverPrice());
            orderDTOS.add(orderGeneralDTO);
        });

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllReceiveOrdersNotPayment(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersSet = orderRepository.findOrdersWithBooksCreatedByUserAndPaymentStatus(user, user.getEmail(),Status.ON_GOING);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        ordersSet.forEach(orders -> {
            OrderGeneralDTO orderGeneralDTO = convertToOrderGeneralDTO(orders, orders.getReceiverPrice());
            orderDTOS.add(orderGeneralDTO);
        });

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllReceiveOrdersFinish(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersSet = orderRepository.findOrdersWithBooksCreatedByUser(user, OrderStatus.FINISH);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        ordersSet.forEach(orders -> {
            OrderGeneralDTO orderGeneralDTO = convertToOrderGeneralDTO(orders, orders.getReceiverPrice());
            orderDTOS.add(orderGeneralDTO);
        });

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllReceiveOrdersWaitingShipper(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersSet = orderRepository.findOrdersWithBooksCreatedByUser(user, OrderStatus.WAITING_SHIPPER);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        ordersSet.forEach(orders -> {
            OrderGeneralDTO orderGeneralDTO = convertToOrderGeneralDTO(orders, orders.getReceiverPrice());
            orderDTOS.add(orderGeneralDTO);
        });

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getAllReceiveOrdersCancel(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<Orders> ordersSet = orderRepository.findOrdersWithBooksCreatedByUser(user, OrderStatus.CANCEL);
        Set<OrderGeneralDTO> orderDTOS = new HashSet<>();
        ordersSet.forEach(orders -> {
            OrderGeneralDTO orderGeneralDTO = convertToOrderGeneralDTO(orders, orders.getReceiverPrice());
            orderDTOS.add(orderGeneralDTO);
        });

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getRequestUserOrdersNotPay(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        Set<OrderGeneralDTO> orderDTOSet = new HashSet<>();
        Set<Orders> ordersList = orderRepository.findOrdersByCreateByAndPayment(user);

        for (Orders orders: ordersList
        ) {
            OrderGeneralDTO orderDTO = convertToOrderGeneralDTO(orders,orders.getSenderPrice());
            orderDTOSet.add(orderDTO);
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, orderDTOSet));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getOrderDetail(UUID orderId) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found!"));

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Successfully",null, convertToDTO(orders)));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> cancelRequestOrderNotConfirm(Principal principal, UUID orderId) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found!"));

        cancelRequestOrderNotConfirmFunction(principal, orders);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Cancel success"));
    }

    public void cancelRequestOrderNotConfirmFunction(Principal principal, Orders orders) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        if(orders.getOrderStatus().equals(OrderStatus.WAITING_CONFIRM)) {
            orders.setOrderStatus(OrderStatus.CANCEL);

            orders.setUserCancel(user.getEmail());
            paymentService.createCancelOrderPayment(orders);
            //đổi trang thái book không lock nữa
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrders(orders);
            for (OrderDetail orderDetail: orderDetails
                 ) {
                orderDetail.getBook().setLock(false);
            }
            //gở sách khỏi order
            orderDetailRepository.deleteAll(orderDetails);
            //đổi trạng thái post nếu post đang bị lock
            PostStatus locked = postStatusRepository.findByName("LOCKED")
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found!"));
            PostStatus active = postStatusRepository.findByName("ACTIVE")
                    .orElseThrow(() -> new ResourceNotFoundException("Status not found!"));
            if(orders.getPost().getPostStatus() == locked) {
                orders.getPost().setPostStatus(active);
            }
            orderRepository.save(orders);
            for (Payment payment: orders.getPayments()
            ) {
                if(payment.getStatus().equals(Status.ON_GOING)) {
                    paymentRepository.deleteById(payment.getId());
                }

            }

        }
        if(orders.getOrderStatus().equals(OrderStatus.NOT_PAY)) {
            orders.setOrderStatus(OrderStatus.CANCEL);
            orders.setUserCancel(user.getEmail());
            //đổi trang thái book không lock nữa
            List<OrderDetail> orderDetails = orderDetailRepository.findByOrders(orders);
            for (OrderDetail orderDetail: orderDetails
            ) {
                Book book = orderDetail.getBook();
                book.setLock(false);
                bookRepository.saveAndFlush(book);
            }
            //gở sách khỏi order
            orderDetailRepository.deleteAll(orderDetails);
            orderRepository.save(orders);
            // xóa đơn thanh toán
            for (Payment payment: orders.getPayments()
            ) {
                if(payment.getStatus().equals(Status.ON_GOING)) {
                    paymentRepository.deleteById(payment.getId());
                }

            }
        }

    }

    public OrderDTO convertToDTO (Orders orders) {
        if(orders == null) {
            return null;
        }
        Set<BookGeneralDTO> bookDTOS = new HashSet<>();
        OrderDTO orderDTO = modelMapper.map(orders, OrderDTO.class);
        orderDTO.setUserOrderDTO(convertToUserOrderDTO(orders));
        orderDTO.setCancelBy(orders.getUserCancel());
        orderDTO.setCity(orders.getArea().getCity());
        orderDTO.setBookPrice(orders.getBookPrice());
        orderDTO.setDistrict(orders.getDistrict().getDistrict());
        PostDTO postDTO = postServiceHelper.convertToDTO(orders.getPost());
        orderDTO.setPostDTO(postDTO);
        for (Book book: orderDetailRepository.findBooksInOrder(orders)
        ) {
            BookGeneralDTO bookDTO = bookService.convertToGeneralDTO(book);
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
    public UserOrderDTO convertToUserOrderDTO (Orders orders) {
        if(orders == null) {
            return null;
        }
        User user = orders.getCreateBy();
        UserOrderDTO userOrderDTO = new UserOrderDTO();
        String name = user.getLastName() + " " + user.getFirstName();
        userOrderDTO.setEmail(user.getEmail());
        userOrderDTO.setId(user.getId());
        userOrderDTO.setPhone(user.getPhone());
        userOrderDTO.setName(name);
        userOrderDTO.setCity(user.getCity());
        userOrderDTO.setDistrict(user.getDistrict());
        return userOrderDTO;
    }
    public OrderGeneralDTO convertToOrderGeneralDTO (Orders orders, BigDecimal price) {
        if(orders == null) {
            return null;
        }
        OrderGeneralDTO orderGeneralDTO = new OrderGeneralDTO();
        orderGeneralDTO.setId(orders.getId());
        orderGeneralDTO.setPrice(price);
        orderGeneralDTO.setCreateDate(orders.getCreateDate());
        orderGeneralDTO.setOrderStatus(orders.getOrderStatus());
        orderGeneralDTO.setConfirm(orders.isConfirm());
        orderGeneralDTO.setPayment(orders.isPayment());
        orderGeneralDTO.setCancelBy(orders.getUserCancel());
        orderGeneralDTO.setUserOrderDTO(convertToUserOrderDTO(orders));
        PostGeneralDTO postGeneralDTO = postServiceHelper.convertToGeneralDTO(orders.getPost());
        orderGeneralDTO.setPostGeneralDTO(postGeneralDTO);
        return orderGeneralDTO;
    }
}

