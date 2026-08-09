package com.pk.payment.dto;

import java.math.BigDecimal;

public record OrderRequest(
        Integer quantity,
        BigDecimal amount,
        String currency
) {
}