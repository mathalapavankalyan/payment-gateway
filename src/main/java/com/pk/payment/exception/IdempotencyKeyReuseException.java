package com.pk.payment.exception;

@SuppressWarnings("serial")
public class IdempotencyKeyReuseException extends RuntimeException {

    public IdempotencyKeyReuseException(String idempotencyKey) {
        super("Idempotency key has already been used with a different request: "
                );
    }
}