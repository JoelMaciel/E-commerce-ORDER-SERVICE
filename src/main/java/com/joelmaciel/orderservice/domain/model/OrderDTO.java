package com.joelmaciel.orderservice.domain.model;

import com.joelmaciel.orderservice.domain.entity.Order;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private UUID orderId;
    private UUID productId;
    private Integer quantity;
    private BigDecimal amount;
    private String status;
    @CreationTimestamp
    private Instant orderDate;

    public static OrderDTO toDTO(Order order) {
        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .productId(order.getProductId())
                .quantity(order.getQuantity())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .amount(order.getAmount())
                .build();
    }
}
