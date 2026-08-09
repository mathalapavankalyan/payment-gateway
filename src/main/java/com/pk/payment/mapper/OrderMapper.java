package com.pk.payment.mapper;

import org.springframework.stereotype.Component;

import com.pk.payment.dto.OrderRequest;
import com.pk.payment.dto.OrderResponse;
import com.pk.payment.entity.Order;

@Component
public class OrderMapper {

    public Order toEntity(OrderRequest request) {
        Order order = new Order();

        order.setQuantity(request.quantity());
        order.setAmount(request.amount());
        order.setCurrency(request.currency());

        return order;
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getOrderId(),
                order.getQuantity(),
                order.getAmount(),
                order.getCurrency(),
                order.getOrderStatus()
        );
    }
}