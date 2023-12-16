package com.joelmaciel.orderservice.domain.exception;

import java.util.UUID;

public class OrderNotFoundException extends EntityNotExistException{
    public OrderNotFoundException(String message) {
        super(message);
    }
    public OrderNotFoundException(UUID orderId) {
        this(String.format("There is no order with this id %s saved in the database", orderId));
    }
}
