package com.example.bookswapplatform.service.impl;

import com.example.bookswapplatform.common.ExchangeMethod;
import com.example.bookswapplatform.dto.*;
import com.example.bookswapplatform.entity.Book.Book;
import com.example.bookswapplatform.entity.Order.OrderShipping;
import com.example.bookswapplatform.entity.Order.OrderStatus;
import com.example.bookswapplatform.entity.Order.Orders;
import com.example.bookswapplatform.entity.Payment.Payment;
import com.example.bookswapplatform.entity.Payment.Status;
import com.example.bookswapplatform.entity.Payment.Transaction.Transaction;
import com.example.bookswapplatform.entity.Payment.Transaction.TransactionType;
import com.example.bookswapplatform.entity.Payment.Transaction.WalletType;
import com.example.bookswapplatform.entity.User.User;
import com.example.bookswapplatform.exception.ResourceNotFoundException;
import com.example.bookswapplatform.repository.*;
import com.example.bookswapplatform.service.DistanceMatrixService;
import com.example.bookswapplatform.service.OrderShippingService;
import com.example.bookswapplatform.utils.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderShippingServiceImpl implements OrderShippingService {
    private final OrderRepository orderRepository;
    private final OrderShippingRepository orderShippingRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentServiceImpl paymentService;
    private final DistanceMatrixService distanceMatrixService;
    private final PostServiceImpl postService;
    private final BookServiceImpl bookService;
    private final OrderDetailRepository orderDetailRepository;
    private final ModelMapper modelMapper;

    @Override
    public ResponseEntity<BaseResponseDTO> createShippingOrder(Principal principal, UUID orderId) {
        Orders orders = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found!"));
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        BigDecimal tradePrice = orders.getShipPrice().divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
        if (orders.isShipping()) {
            if (orders.getPost().getExchangeMethod().equals(ExchangeMethod.TRADE)) {
                OrderShipping orderShipping1 = new OrderShipping();
                OrderShipping orderShipping2 = new OrderShipping();
                orderShipping1.setOrderStatus(orders.getOrderStatus());
                orderShipping1.setOrders(orders);
                orderShipping1.setUser(null);
                orderShipping1.setPrice(tradePrice);


                orderShipping2.setOrderStatus(orders.getOrderStatus());
                orderShipping2.setOrders(orders);
                orderShipping2.setUser(null);
                orderShipping2.setPrice(tradePrice);

                String startLocation = orders.getLocationDetail() + ", " + orders.getDistrict().getDistrict() + ", " + orders.getArea().getCity();
                String finishLocation = orders.getPost().getLocationDetail() + ", " +
                        orders.getPost().getDistrict().getDistrict() + ", " +
                        orders.getPost().getArea().getCity();

                orderShipping1.setCreateBy(user.getId());
                orderShipping1.setUserReceive(orders.getCreateBy().getId());
                orderShipping1.setStartLocation(finishLocation);
                orderShipping1.setFinishLocation(startLocation);

                orderShipping2.setCreateBy(orders.getCreateBy().getId());
                orderShipping2.setUserReceive(user.getId());
                orderShipping2.setStartLocation(startLocation);
                orderShipping2.setFinishLocation(finishLocation);

                orderShippingRepository.save(orderShipping1);
                orderShippingRepository.save(orderShipping2);
            } else {
                OrderShipping orderShipping = new OrderShipping();
                orderShipping.setOrderStatus(OrderStatus.WAITING_SHIPPER);
                orderShipping.setOrders(orders);
                orderShipping.setUser(null);
                orderShipping.setPrice(tradePrice);
                String startLocation = orders.getLocationDetail() + ", " + orders.getDistrict().getDistrict() + ", " + orders.getArea().getCity();
                String finishLocation = orders.getPost().getLocationDetail() + ", " +
                        orders.getPost().getDistrict().getDistrict() + ", " +
                        orders.getPost().getArea().getCity();
                orderShipping.setCreateBy(user.getId());
                orderShipping.setUserReceive(orders.getCreateBy().getId());
                orderShipping.setStartLocation(finishLocation);
                orderShipping.setFinishLocation(startLocation);
                orderShippingRepository.save(orderShipping);
            }
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.CREATED, "Create success"));
        } else {
            return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Not create order shipping"));
        }

    }

    @Override
    public ResponseEntity<BaseResponseDTO> getOrderShipping(Principal principal, String status) {
        OrderStatus orderStatus = OrderStatus.valueOf(status);
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        List<OrderShipping> orderShippings = orderShippingRepository.findByOrderStatusAndCreateBy(user, orderStatus);
        List<OrderShippingDTO> orderShippingDTOS = new ArrayList<>();
        if (orderShippings.isEmpty()) {
            orderShippingDTOS = null;
        } else {
            for (OrderShipping orderShipping : orderShippings
            ) {
                orderShippingDTOS.add(convertToDTO(orderShipping));
            }
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, orderShippingDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> takeOrderShipping(Principal principal, UUID orderShippingId) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        OrderShipping orderShipping = orderShippingRepository.findById(orderShippingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping order not found!"));
        Orders orders = orderShipping.getOrders();
        if (!orderShipping.getOrderStatus().equals(OrderStatus.WAITING_SHIPPER)) {
            return ResponseEntity.badRequest()
                    .body(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.BAD_REQUEST, "Shipping order is not waiting shipper"));
        }
        orderShipping.setUser(user);
        orderShipping.setStartDate(LocalDateTime.now());
        orderShipping.setFinishDate(null);
        orderShipping.setOrderStatus(OrderStatus.PREPARING);
        orderShippingRepository.saveAndFlush(orderShipping);

        List<OrderShipping> orderShippings = orderShippingRepository.findByOrder(orders);
        boolean allPreparing = orderShippings
                .stream()
                .allMatch(orderShippingCheck -> orderShippingCheck.getOrderStatus().equals(OrderStatus.PREPARING));
        if (allPreparing) {
            orders.setOrderStatus(OrderStatus.PREPARING);
        } else {
            orders.setOrderStatus(OrderStatus.WAITING_SHIPPER);
        }

        orderRepository.save(orders);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success"));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getById(UUID orderShippingId) {
        OrderShipping orderShipping = orderShippingRepository.findById(orderShippingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping order not found!"));
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, convertToDTO(orderShipping)));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> changeOrderStatus(Principal principal, UUID orderShippingId, String status) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        OrderShipping orderShipping = orderShippingRepository.findById(orderShippingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping order not found!"));
        Orders orders = orderShipping.getOrders();
        ExchangeMethod exchangeMethod = orders.getPost().getExchangeMethod();
        if (status.equals("FINISH")) {
            if (exchangeMethod.equals(ExchangeMethod.TRADE) &&
                    orders.getShippingOrders().stream().anyMatch(orderShipping1 -> orderShipping1.getOrderStatus().equals(OrderStatus.FINISH))) {
                orders.setOrderStatus(OrderStatus.FINISH);
                orderRepository.save(orders);

            }
            if (exchangeMethod.equals(ExchangeMethod.SELL) || exchangeMethod.equals(ExchangeMethod.GIVE)) {
                orders.setOrderStatus(OrderStatus.FINISH);
                orderRepository.save(orders);
                if (orders.getPost().getExchangeMethod().equals(ExchangeMethod.SELL)) {
                    User poster = orders.getPost().getCreateBy();
                    BigDecimal balance = poster.getUserWallet().getBalance();
                    BigDecimal newPosterBalance = balance.add(orders.getBookPrice());
                    poster.getUserWallet().setBalance(newPosterBalance);
                    paymentService.createReceiveSellTransaction(orders, poster);
                }
            }
            processShipperPrice(orderShipping, user);
            orderShipping.setFinishDate(LocalDateTime.now());
        }

        if (status.equals("ON_GOING")) {
            //xu ly luong shipper giao cung luc 2 don ship
            List<OrderShipping> orderShippingList = orderShippingRepository.findByUserAndOrders(user, orders);
            Set<OrderShipping> orderShippingListInOrder = orders.getShippingOrders();
            if (orders.getPost().getExchangeMethod().equals(ExchangeMethod.TRADE)) {
                if (orderShippingList.size() != 2) {
                    //xu ly luong shipper chi nhan 1 don ship
                    if (orderShippingListInOrder.stream().anyMatch(orderShipping1 -> orderShipping1.getOrderStatus().equals(OrderStatus.HOLDING))) {
                        //orderShipping.setOrderStatus(OrderStatus.valueOf(status));
                        //doi status order shipping con lai
                        OrderShipping remainingOrderShipping = orderShippingListInOrder.stream()
                                .filter(orderShipping1 -> !orderShipping1.getId().equals(orderShipping.getId()))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException("Remaining order not found!"));
                        remainingOrderShipping.setOrderStatus(OrderStatus.ON_GOING);
                        orderShippingRepository.save(remainingOrderShipping);
                    } else {
                        status = "HOLDING";
                    }
                } else {
                    if (orderShippingList.stream().anyMatch(orderShipping1 -> orderShipping1.getOrderStatus().equals(OrderStatus.HOLDING))) {
                        OrderShipping remainingOrderShipping = orderShippingList.stream()
                                .filter(orderShipping1 -> !orderShipping1.getId().equals(orderShipping.getId()))
                                .findFirst()
                                .orElseThrow(() -> new ResourceNotFoundException("Remaining order not found!"));
                        remainingOrderShipping.setOrderStatus(OrderStatus.ON_GOING);
                        orderShippingRepository.save(remainingOrderShipping);
                    }
                }
            }
        }

        orderShipping.setOrderStatus(OrderStatus.valueOf(status));
        orderShippingRepository.save(orderShipping);
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success"));
    }

    public void processShipperPrice(OrderShipping orderShipping, User user) {
        BigDecimal percentPay = new BigDecimal(orderShipping.getPercentPay())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal shipperPrice = orderShipping.getPrice().multiply(percentPay);
        Payment payment = new Payment();
        payment.setAmount(shipperPrice);
        payment.setCreateBy("Book Swap");
        payment.setStatus(Status.SUCCESS);
        payment.setOrders(orderShipping.getOrders());
        paymentRepository.save(payment);
        createTransaction(payment, user);
        BigDecimal shipperBalance = user.getUserWallet().getBalance();
        BigDecimal newShipperBalance = shipperBalance.add(shipperPrice);
        user.getUserWallet().setBalance(newShipperBalance);
    }

    public void createPayment(BigDecimal shipperPrice, User user) {
        Payment payment = new Payment();
        payment.setFee(BigDecimal.valueOf(0));
        payment.setAmount(shipperPrice);
        payment.setCreateBy("Book Swap");
        payment.setStatus(Status.SUCCESS);
    }

    public void createTransaction(Payment payment, User user) {
        Transaction transaction = new Transaction();
        transaction.setAmount(payment.getAmount());
        transaction.setFee(payment.getFee());
        transaction.setStatus(String.valueOf(payment.getStatus()));
        transaction.setWalletType(WalletType.BOOKSWAP);
        transaction.setTransactionType(TransactionType.RECEIVE);
        transaction.setCreateBy(user.getEmail());
        transaction.setPayment(payment);
        transaction.setDescription("Nhận tiền từ " + payment.getCreateBy());
        transactionRepository.save(transaction);
    }

    @Override
    public ResponseEntity<BaseResponseDTO> filter(String keyWord, String district) {
        List<OrderShippingGeneralDTO> orderShippingGeneralDTOList = new ArrayList<>();
        List<Orders> ordersList = new ArrayList<>();
        if (keyWord == null && district == null) {
            ordersList = orderRepository.findByOrderStatus(OrderStatus.WAITING_SHIPPER);
            //List<OrderShipping> orderShippingList = orderShippingRepository.findByOrder(orders);
        } else {
            if (district == null) {
                ordersList = orderRepository.findByKeyWord(keyWord);
            } else {
                ordersList = orderRepository.findByDistrict(district);
            }

        }
        for (Orders order : ordersList
        ) {
            orderShippingGeneralDTOList.add(convertToGeneralDTO(order));
        }

        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, orderShippingGeneralDTOList));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> trackOrderShipping(Principal principal, UUID orderShippingId) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        OrderShipping orderShipping = orderShippingRepository.findById(orderShippingId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping order not found!"));
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, convertToDTO(orderShipping)));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> getOrderShippingForUser(Principal principal, String type) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        List<OrderShipping> orderShippingList = new ArrayList<>();
        List<OrderShippingDTO> orderShippingDTOS = new ArrayList<>();
        if (type.equals("SEND")) {
            orderShippingList = orderShippingRepository.findByCreateBy(user.getId());
        } else {
            orderShippingList = orderShippingRepository.findByUserReceive(user.getId());
        }
        for (OrderShipping orderShipping : orderShippingList
        ) {
            orderShippingDTOS.add(convertToDTO(orderShipping));
        }
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, orderShippingDTOS));
    }

    @Override
    public ResponseEntity<BaseResponseDTO> statisticRevenueInDay(Principal principal) {
        User user = userRepository.findByFireBaseUid(principal.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        List<OrderShipping> orderShippings = orderShippingRepository.findByOrderStatusAndCreateBy(user, OrderStatus.FINISH);
        List<Transaction> transactions = transactionRepository.findAllByCreateBy(user.getEmail());
        LocalDate currentDate = LocalDate.now();
        // Calculate total revenue for the day
        BigDecimal totalRevenue = transactions.stream()
                .filter(transaction -> transaction.getCreateDate().toLocalDate().equals(currentDate))
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        RevenueShipOrderDTO revenueShipOrderDTO = new RevenueShipOrderDTO(totalRevenue, orderShippings.size());
        return ResponseEntity.ok(new BaseResponseDTO(LocalDateTime.now(), HttpStatus.OK, "Success", null, revenueShipOrderDTO));
    }

    private OrderShippingDTO convertToDTO(OrderShipping orderShipping) {
        if (orderShipping == null) {
            return null;
        }
        Orders orders = orderShipping.getOrders();
        OrderShippingDTO orderShippingDTO = modelMapper.map(orderShipping, OrderShippingDTO.class);
        User userSend = userRepository.findById(orderShipping.getCreateBy())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        User userReceive = userRepository.findById(orderShipping.getUserReceive())
                .orElseThrow(() -> new ResourceNotFoundException("User not found!"));
        User shipper = orderShipping.getUser();
        orderShippingDTO.setUserSend(postService.convertToUserGeneralDTO(userSend));
        orderShippingDTO.setUserReceive(postService.convertToUserGeneralDTO(userReceive));
        orderShippingDTO.setShipper(postService.convertToUserGeneralDTO(shipper));
        orderShippingDTO.setPostGeneralDTO(postService.convertToGeneralDTO(orders.getPost()));
        Set<BookGeneralDTO> bookDTOS = new HashSet<>();
        for (Book book : orderDetailRepository.findBooksInOrder(orders)
        ) {
            BookGeneralDTO bookDTO = bookService.convertToGeneralDTO(book);
            bookDTOS.add(bookDTO);
        }
        orderShippingDTO.setBookTradeDTOS(bookDTOS);
        orderShippingDTO.setShipPrice(orders.getReceiverShipPrice().add(orders.getSenderShipPrice()));
        return orderShippingDTO;
    }

    private OrderShippingGeneralDTO convertToGeneralDTO(Orders orders) {
        if (orders == null) {
            return null;
        }
        OrderShippingGeneralDTO orderShippingGeneralDTO = new OrderShippingGeneralDTO();
        Set<OrderShippingDTO> orderShippingDTOS = new HashSet<>();
        Set<OrderShipping> orderShippings = orders.getShippingOrders();
        for (OrderShipping orderShipping : orderShippings
        ) {
            if (orderShipping.getOrderStatus().equals(OrderStatus.WAITING_SHIPPER)) {
                orderShippingDTOS.add(convertToDTO(orderShipping));
            }
        }
        orderShippingGeneralDTO.setOrderShippingDTOS(orderShippingDTOS);

        PostGeneralDTO postGeneralDTO = postService.convertToGeneralDTO(orders.getPost());
        orderShippingGeneralDTO.setPostGeneralDTO(postGeneralDTO);
        orderShippingGeneralDTO.setCreateDate(orders.getCreateDate());
        orderShippingGeneralDTO.setOrderStatus(orders.getOrderStatus());
        return orderShippingGeneralDTO;
    }
}
