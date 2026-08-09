package com.pk.payment.service.impl;

import org.springframework.stereotype.Service;

import com.pk.payment.dto.OrderRequest;
import com.pk.payment.dto.OrderResponse;
import com.pk.payment.entity.Order;
import com.pk.payment.enums.OrderStatus;
import com.pk.payment.exception.OrderNotFoundException;
import com.pk.payment.mapper.OrderMapper;
import com.pk.payment.repository.OrderRepository;
import com.pk.payment.service.OrderService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;

    @Override
    public OrderResponse createOrder(OrderRequest request) {
        Order order = orderMapper.toEntity(request);
        order.setOrderStatus(OrderStatus.CREATED);

        Order savedOrder = orderRepository.save(order);

        return orderMapper.toResponse(savedOrder);
    }

    @Override
    public OrderResponse getOrder(Long orderId) {
    	Order order = orderRepository.findById(orderId)
    	        .orElseThrow(() -> new OrderNotFoundException(orderId));

    	return orderMapper.toResponse(order);
    }
}