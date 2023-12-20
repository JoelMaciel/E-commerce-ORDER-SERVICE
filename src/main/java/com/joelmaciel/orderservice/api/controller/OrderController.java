package com.joelmaciel.orderservice.api.controller;

import com.joelmaciel.orderservice.domain.model.OrderDTO;
import com.joelmaciel.orderservice.domain.model.OrderRequest;
import com.joelmaciel.orderservice.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Page<OrderDTO> getAllOrders(
            @PageableDefault(page = 0, size = 10, sort = "orderId",direction = Sort.Direction.ASC) Pageable pageable ,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) String status) {
        return orderService.findAllOrders(pageable,productId,status);
    }

    @PreAuthorize("hasAuthority('Admin') || hasAuthority('Customer')")
    @GetMapping("/{orderId}")
    public OrderDTO getOrderDetails(@PathVariable UUID orderId) {
        return orderService.getOrderDetails(orderId);
    }

    @PreAuthorize("hasAuthority('Customer') || hasAuthority('SCOPE_internal')")
    @PostMapping("/placeOrders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDTO placeOrder(@RequestBody @Valid OrderRequest orderRequest) {
        return orderService.savePlaceOrder(orderRequest);
    }
}
