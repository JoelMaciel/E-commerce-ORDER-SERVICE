package com.joelmaciel.orderservice.domain.exception;

public abstract class EntityNotExistException extends BusinessException {

    public EntityNotExistException(String message) {
        super(message);
    }
}
