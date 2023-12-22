package com.joelmaciel.orderservice.domain.service.impl;

import com.joelmaciel.orderservice.api.openfeign.client.PaymentService;
import com.joelmaciel.orderservice.api.openfeign.client.ProductService;
import com.joelmaciel.orderservice.api.openfeign.request.PaymentRequestDTO;
import com.joelmaciel.orderservice.api.openfeign.response.TransactionDetailsDTO;
import com.joelmaciel.orderservice.domain.OrderRepository;
import com.joelmaciel.orderservice.domain.entity.Order;
import com.joelmaciel.orderservice.domain.enums.OrderStatus;
import com.joelmaciel.orderservice.domain.exception.OrderNotFoundException;
import com.joelmaciel.orderservice.domain.exception.PaymentServiceException;
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
import java.util.Objects;
import java.util.UUID;


@Log4j2
@RequiredArgsConstructor
@Service
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
        Order order = createOrderFromRequest(orderRequest);
        processPayment(order, orderRequest);

        log.info("Order Placed successfully with Quantity: {}", order.getQuantity());
        return OrderDTO.toDTO(order);
    }

    private Order createOrderFromRequest(OrderRequest orderRequest) {
        Order order = OrderRequest.toEntity(orderRequest);
        ProductDTO productDTO = fetchProduct(order.getProductId());
        BigDecimal amount = calculateOrderAmount(productDTO, order.getQuantity());

        order.setAmount(amount);
        orderRepository.save(order);
        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("Order with Status CREATED");
        return order;
    }

    private ProductDTO fetchProduct(UUID productId) {
        return restTemplate.getForObject(API_PRODUCTS + productId, ProductDTO.class);
    }

    private BigDecimal calculateOrderAmount(ProductDTO productDTO, int quantity) {
        return Objects.requireNonNull(productDTO).getPrice().multiply(new BigDecimal(quantity));
    }

    private void processPayment(Order order, OrderRequest orderRequest) {
        PaymentRequestDTO paymentRequestDTO = buildPaymentRequestDTO(order, orderRequest, order.getAmount());

        try {
            paymentService.savePayment(paymentRequestDTO);
            order.setStatus(OrderStatus.PLACED);
            log.info("Payment done Successfully. Order status changed to PLACED");
        } catch (PaymentServiceException e) {
            order.setStatus(OrderStatus.PAYMENT_FAILED);
            log.error("Error occurred in payment. Order status changed to PAYMENT_FAILED", e);
        }
    }

    private PaymentRequestDTO buildPaymentRequestDTO(Order order, OrderRequest orderRequest, BigDecimal amount) {
        return PaymentRequestDTO.builder()
                .orderId(order.getOrderId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(amount)
                .build();
    }

}