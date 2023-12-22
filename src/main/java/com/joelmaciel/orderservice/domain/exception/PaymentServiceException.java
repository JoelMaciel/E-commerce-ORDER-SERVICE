package com.joelmaciel.orderservice.domain.exception;

public class PaymentServiceException extends BusinessException{
    public PaymentServiceException(String message) {
        super("Payment could not be processed");
    }

    public PaymentServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
