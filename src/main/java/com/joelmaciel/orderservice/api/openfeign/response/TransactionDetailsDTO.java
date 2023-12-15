package com.joelmaciel.orderservice.api.openfeign.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDetailsDTO {

    private UUID transactionId;
    private UUID orderId;
    private String paymentMode;
    private String referenceNumber;
    private Instant paymentDate;
    private String status;
    private BigDecimal amount;
}
