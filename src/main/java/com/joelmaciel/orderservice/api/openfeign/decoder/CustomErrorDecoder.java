package com.joelmaciel.orderservice.api.openfeign.decoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.joelmaciel.orderservice.api.openfeign.response.ErrorResponse;
import com.joelmaciel.orderservice.domain.exception.CustomException;
import com.joelmaciel.orderservice.domain.exception.ProductInsufficientQuantityException;
import com.joelmaciel.orderservice.domain.exception.ProductNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;


@Log4j2
public class CustomErrorDecoder implements ErrorDecoder {


    @Override
    public Exception decode(String s, Response response) {
        ObjectMapper objectMapper = new ObjectMapper();

        log.info("::{}", response.request().url());
        log.info("::{}", response.request().headers());
        try {
            ErrorResponse errorResponse = objectMapper.readValue(response.body().asInputStream(), ErrorResponse.class);
            return new CustomException(errorResponse.getErrorMessage(), errorResponse.getErrorCode(), response.status());
        } catch (IOException | ProductNotFoundException | ProductInsufficientQuantityException e) {
            throw new CustomException("Order quantity  is greater than stock OR", "Product with given uuid not found", 400);
        }
    }
}
