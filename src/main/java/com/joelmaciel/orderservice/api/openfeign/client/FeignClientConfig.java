package com.joelmaciel.orderservice.api.openfeign.client;

import com.joelmaciel.orderservice.api.openfeign.decoder.CustomErrorDecoder;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignClientConfig {
    @Bean
    public ErrorDecoder erroDecoder() {
        return new CustomErrorDecoder();
    }

}
