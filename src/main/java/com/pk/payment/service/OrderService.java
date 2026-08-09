package com.pk.payment.service;

import com.pk.payment.dto.OrderRequest;
import com.pk.payment.dto.OrderResponse;

public interface OrderService {

    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrder(Long orderId);
}