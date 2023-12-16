package com.joelmaciel.orderservice.domain.service.impl;

import com.joelmaciel.orderservice.api.openfeign.client.PaymentService;
import com.joelmaciel.orderservice.api.openfeign.client.ProductService;
import com.joelmaciel.orderservice.api.openfeign.request.PaymentRequestDTO;
import com.joelmaciel.orderservice.api.openfeign.response.TransactionDetailsDTO;
import com.joelmaciel.orderservice.domain.OrderRepository;
import com.joelmaciel.orderservice.domain.entity.Order;
import com.joelmaciel.orderservice.domain.exception.OrderNotFoundException;
import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;
import com.joelmaciel.orderservice.domain.model.ProductDTO;
import com.joelmaciel.orderservice.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    public static final String API_PRODUCTS = "http://PRODUCT-SERVICE/api/products/";
    public static final String API_PAYMENTS = "http://PAYMENT-SERVICE/api/payments/orders/";
    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;
    private final RestTemplate restTemplate;

    @Override
    @Transactional(readOnly = true)
    public Page<OrderDTO> findAllOrders(Pageable pageable, UUID productId, String status) {
        Page<Order> orders;
        if (productId != null) {
            orders = orderRepository.findByProductId(productId, pageable);
        } else if (status != null) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(OrderDTO::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderDetails(UUID orderId) {
        log.info("Get order details for Order id : {}", orderId);
        Order order = findOrderById(orderId);

        log.info("Invoking Product service to fetch the product for id: {}", order.getProductId());
        ProductDTO productDTO = restTemplate.getForObject(API_PRODUCTS + order.getProductId(), ProductDTO.class);

        log.info("Getting payment information for the payment service");
        TransactionDetailsDTO transactionDetails = restTemplate.getForObject(
                API_PAYMENTS + order.getOrderId(), TransactionDetailsDTO.class
        );


        OrderDTO.ProductDetails productDetails = OrderDTO.ProductDetails.builder()
                .name(productDTO.getName())
                .price(productDTO.getPrice())
                .build();

        OrderDTO orderDTO = OrderDTO.toDTO(order);
        orderDTO.setProductDetails(productDetails);
        orderDTO.setTransactionDetails(transactionDetails);
        return orderDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public Order findOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Override
    @Transactional
    public OrderDTO savePlaceOrder(OrderRequest orderRequest) {
        log.info("Placing Order Request: {}", orderRequest);
        Order order = OrderRequest.toEntity(orderRequest);

        ProductDTO productDTO = restTemplate.getForObject(API_PRODUCTS + order.getProductId(), ProductDTO.class);

        BigDecimal amount = productDTO.getPrice().multiply(new BigDecimal(order.getQuantity()));
        order.setAmount(amount);
        orderRepository.save(order);

        log.info("Creating Order with Status CREATED");
        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("Calling Payment Service to complete the payment");
        PaymentRequestDTO paymentRequestDTO = PaymentRequestDTO.builder()
                .orderId(order.getOrderId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(amount)
                .build();

        String orderStatus = null;
        try {
            paymentService.savePayment(paymentRequestDTO);
            log.info("Payment done Successfully. Changing the Order status to PLACED");
            orderStatus = "PLACED";
        } catch (Exception e) {
            log.info("Error occurred in payment. Changing order status to PAYMENT_FAILED");
            orderStatus = "PAYMENT_FAILED";
        }
        order.setStatus(orderStatus);

        log.info("Order Placed successfully with Quantity: {}", order.getQuantity());
        return OrderDTO.toDTO(order);
    }

}