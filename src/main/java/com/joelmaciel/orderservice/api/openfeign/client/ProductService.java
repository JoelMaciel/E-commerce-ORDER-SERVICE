package com.joelmaciel.orderservice.api.openfeign.client;

import com.joelmaciel.orderservice.domain.exception.CustomException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;
@CircuitBreaker(name = "external", fallbackMethod = "fallback")
@FeignClient(name = "PRODUCT-SERVICE/api/products", configuration = FeignClientConfig.class)
public interface ProductService {

    @PutMapping("/reduceQuantity/{productId}")
    void reduceQuantity(@PathVariable UUID productId, @RequestParam Integer quantity);

    default void fallback(Exception e) {
        throw new CustomException("Product Service is not available", "UNAVAILABLE", 500);
    }
}
