package com.joelmaciel.orderservice.api.openfeign.client;

import com.joelmaciel.orderservice.api.openfeign.request.PaymentRequestDTO;
import com.joelmaciel.orderservice.api.openfeign.response.TransactionDetailsDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.Valid;

@FeignClient(name = "PAYMENT-SERVICE/api/payments", configuration = FeignClientConfig.class)
public interface PaymentService {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TransactionDetailsDTO savePayment(@RequestBody @Valid PaymentRequestDTO paymentRequestDTO);
}
