package com.pk.payment.dto;

import com.pk.payment.enums.PaymentType;

public record PaymentRequest(
        Long orderId,
        PaymentType paymentType
) {
}