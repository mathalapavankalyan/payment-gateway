package com.pk.payment.exception;


@SuppressWarnings("serial")
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("Order not found with id: " + orderId);
    }
}