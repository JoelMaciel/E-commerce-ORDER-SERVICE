package com.joelmaciel.orderservice.domain.service.impl;

import com.joelmaciel.orderservice.api.openfeign.client.PaymentService;
import com.joelmaciel.orderservice.api.openfeign.client.ProductService;
import com.joelmaciel.orderservice.api.openfeign.request.PaymentRequest;
import com.joelmaciel.orderservice.domain.OrderRepository;
import com.joelmaciel.orderservice.domain.entity.Order;
import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;
import com.joelmaciel.orderservice.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final ProductService productService;
    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    @Override
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