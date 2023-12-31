package com.joelmaciel.orderservice.domain.entity;

import com.joelmaciel.orderservice.domain.enums.OrderStatus;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @EqualsAndHashCode.Include
    @Type(type = "uuid-char")
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID orderId;
    @Type(type = "uuid-char")
    private UUID productId;
    private Integer quantity;
    @CreationTimestamp
    private Instant orderDate;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private BigDecimal amount;
}
