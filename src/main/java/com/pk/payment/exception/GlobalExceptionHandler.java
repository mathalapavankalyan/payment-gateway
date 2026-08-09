package com.pk.payment.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import com.pk.payment.dto.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(
            OrderNotFoundException exception,
            WebRequest request) {

        ErrorResponse response = new ErrorResponse(
                "ORDER_NOT_FOUND",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(
            PaymentNotFoundException exception,
            WebRequest request) {

        ErrorResponse response = new ErrorResponse(
                "PAYMENT_NOT_FOUND",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(InvalidOrderStateException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOrderState(
            InvalidOrderStateException exception,
            WebRequest request) {

        ErrorResponse response = new ErrorResponse(
                "INVALID_ORDER_STATE",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
    
    
    @ExceptionHandler(IdempotencyKeyReuseException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyKeyReuse(
            IdempotencyKeyReuseException exception,
            WebRequest request) {

        ErrorResponse response = new ErrorResponse(
                "IDEMPOTENCY_KEY_REUSE",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
    
    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessing(
            PaymentProcessingException exception,
            WebRequest request) {

        ErrorResponse response = new ErrorResponse(
                "PAYMENT_PROCESSING",
                exception.getMessage(),
                LocalDateTime.now(),
                request.getDescription(false)
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }
}