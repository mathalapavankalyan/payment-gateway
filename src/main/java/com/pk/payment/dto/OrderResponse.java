package com.pk.payment.dto;

import java.math.BigDecimal;

import com.pk.payment.enums.OrderStatus;

public record OrderResponse(
        Long orderId,
        Integer quantity,
        BigDecimal amount,
        String currency,
        OrderStatus orderStatus
) {
}