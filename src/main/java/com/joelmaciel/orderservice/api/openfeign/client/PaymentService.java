package com.joelmaciel.orderservice.api.openfeign.client;

import com.joelmaciel.orderservice.api.openfeign.request.PaymentRequestDTO;
import com.joelmaciel.orderservice.api.openfeign.response.TransactionDetailsDTO;
import com.joelmaciel.orderservice.domain.exception.CustomException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.Valid;

@CircuitBreaker(name = "external", fallbackMethod = "fallback")
@FeignClient(name = "PAYMENT-SERVICE/api/payments", configuration = FeignClientConfig.class)
public interface PaymentService {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TransactionDetailsDTO savePayment(@RequestBody @Valid PaymentRequestDTO paymentRequestDTO);

    default void fallback(Exception e) {
        throw new CustomException("Payment Service is not available", "UNAVAILABLE", 500);
    }
}
