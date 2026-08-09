package com.pk.payment.exception;

@SuppressWarnings("serial")
public class PaymentProcessingException extends RuntimeException {

    public PaymentProcessingException(String message) {
        super(message);
    }
}