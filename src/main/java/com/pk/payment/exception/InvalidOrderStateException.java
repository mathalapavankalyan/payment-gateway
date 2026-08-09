package com.pk.payment.exception;

@SuppressWarnings("serial")
public class InvalidOrderStateException extends RuntimeException {

    public InvalidOrderStateException(Long orderId, String status) {
        super("Order with id " + orderId
                + " cannot accept payment in state: " + status);
    }
}