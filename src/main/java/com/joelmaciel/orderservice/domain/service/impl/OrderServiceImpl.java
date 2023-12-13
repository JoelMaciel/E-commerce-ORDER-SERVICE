package com.joelmaciel.orderservice.domain.service.impl;

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

    private final OrderRepository orderRepository;

    @Override
    public OrderDTO savePlaceOrder(OrderRequest orderRequest) {
        log.info("Placing Order Request: {}", orderRequest);
        Order order = OrderRequest.toEntity(orderRequest);
        log.info("Order Places successfully with Order UUID: {}", order.getOrderId());
        return OrderDTO.toDTO(orderRepository.save(order));
    }
}
