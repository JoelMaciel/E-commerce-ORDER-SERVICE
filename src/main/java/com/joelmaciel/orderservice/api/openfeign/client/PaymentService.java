package com.joelmaciel.orderservice.api.openfeign.client;

import com.joelmaciel.orderservice.api.openfeign.request.PaymentRequestDTO;
import com.joelmaciel.orderservice.api.openfeign.response.TransactionDetailsDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@CircuitBreaker(name = "external")
@FeignClient(name = "PAYMENT-SERVICE/api/payments")
public interface PaymentService {

    @PostMapping
    TransactionDetailsDTO savePayment(@RequestBody PaymentRequestDTO paymentRequestDTO);

}
