package com.joelmaciel.orderservice.domain.model;

import com.joelmaciel.orderservice.domain.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    @NotNull
    private UUID productId;
    @NotNull
    @PositiveOrZero
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @NotNull
    private PaymentMode paymentMode;

    public static Order toEntity(OrderRequest orderRequest) {
        return Order.builder()
                .productId(orderRequest.productId)
                .orderDate(Instant.now())
                .status("CREATED")
                .quantity(orderRequest.getQuantity())
                .build();
    }
}
