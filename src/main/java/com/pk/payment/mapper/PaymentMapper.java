package com.pk.payment.mapper;

import org.springframework.stereotype.Component;

import com.pk.payment.dto.PaymentRequest;
import com.pk.payment.dto.PaymentResponse;
import com.pk.payment.entity.Order;
import com.pk.payment.entity.Payment;
import com.pk.payment.enums.PaymentStatus;

@Component
public class PaymentMapper {

    public Payment toEntity(
            PaymentRequest request,
            Order order) {

        Payment payment = new Payment();

        payment.setPaymentType(request.paymentType());
        payment.setAmount(order.getAmount());
        payment.setCurrency(order.getCurrency());
        payment.setPaymentStatus(PaymentStatus.INITIATED);

        return payment;
    }

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getPaymentId(),
                payment.getOrder().getOrderId(),
                payment.getPaymentType(),
                payment.getPaymentStatus(),
                payment.getAmount(),
                payment.getCurrency()
        );
    }
}