package com.joelmaciel.orderservice.domain.service;

import com.joelmaciel.orderservice.domain.entity.Order;
import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {
    OrderDTO savePlaceOrder(OrderRequest orderRequest);

    OrderDTO getOrderDetails(UUID orderId);
    Order findOrderById(UUID orderId);

    Page<OrderDTO> findAllOrders(Pageable pageable,UUID productId, String status);
}
