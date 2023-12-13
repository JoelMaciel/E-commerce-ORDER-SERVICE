package com.joelmaciel.orderservice.api.controller;

import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;
import com.joelmaciel.orderservice.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/placeOrders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO placeOrder(@RequestBody @Valid OrderRequest orderRequest) {
        return orderService.savePlaceOrder(orderRequest);
    }
}
