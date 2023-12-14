package com.joelmaciel.orderservice.api.openfeign.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "PRODUCT-SERVICE/api/products", configuration = FeignClientConfig.class)
public interface ProductService {

    @PutMapping("/reduceQuantity/{productId}")
    void reduceQuantity(@PathVariable UUID productId, @RequestParam Integer quantity);
}
