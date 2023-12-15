package com.joelmaciel.orderservice.api.openfeign.request;

import com.joelmaciel.orderservice.domain.model.PaymentMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentRequest {

    private UUID orderId;
    private BigDecimal amount;
    private String referenceNumber;
    private PaymentMode paymentMode;

}
