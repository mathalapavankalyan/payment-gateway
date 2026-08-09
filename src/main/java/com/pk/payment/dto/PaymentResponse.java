package com.pk.payment.dto;

import java.math.BigDecimal;

import com.pk.payment.enums.PaymentStatus;
import com.pk.payment.enums.PaymentType;

public record PaymentResponse(
        Long paymentId,
        Long orderId,
        PaymentType paymentType,
        PaymentStatus paymentStatus,
        BigDecimal amount,
        String currency
) {
}