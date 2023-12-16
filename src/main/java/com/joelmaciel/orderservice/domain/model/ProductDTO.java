package com.joelmaciel.orderservice.domain.model;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {

    private UUID productId;
    private String name;
    private BigDecimal price;
    private Integer quantity;

}
