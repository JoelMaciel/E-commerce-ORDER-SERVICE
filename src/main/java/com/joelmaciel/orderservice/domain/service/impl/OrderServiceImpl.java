package com.joelmaciel.orderservice.domain.service.impl;

import com.joelmaciel.orderservice.api.openfeign.client.PaymentService;
import com.joelmaciel.orderservice.api.openfeign.client.ProductService;
import com.joelmaciel.orderservice.api.openfeign.request.PaymentRequest;
import com.joelmaciel.orderservice.domain.OrderRepository;
import com.joelmaciel.orderservice.domain.entity.Order;
import com.joelmaciel.orderservice.domain.exception.OrderNotFoundException;
import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;
import com.joelmaciel.orderservice.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

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
        return OrderDTO.toDTO(findOrderById(orderId));
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
        orderRepository.save(order);

        log.info("Creating Order with Status CREATED");
        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("Calling Payment Service to complete the payment");
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getOrderId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(orderRequest.getTotalAmount())
                .build();

        String orderStatus = null;
        try {
            paymentService.doPayment(paymentRequest);
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