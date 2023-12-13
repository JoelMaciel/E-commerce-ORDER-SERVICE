package com.joelmaciel.orderservice.domain.service;

import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;

public interface OrderService {
    OrderDTO savePlaceOrder(OrderRequest orderRequest);
}
