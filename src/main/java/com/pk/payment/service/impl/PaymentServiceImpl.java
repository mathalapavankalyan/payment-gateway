package com.pk.payment.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pk.payment.dto.PaymentRequest;
import com.pk.payment.dto.PaymentResponse;
import com.pk.payment.entity.IdempotencyRecord;
import com.pk.payment.entity.Order;
import com.pk.payment.entity.Payment;
import com.pk.payment.enums.IdempotencyStatus;
import com.pk.payment.enums.PaymentStatus;
import com.pk.payment.exception.IdempotencyKeyReuseException;
import com.pk.payment.exception.InvalidOrderStateException;
import com.pk.payment.exception.OrderNotFoundException;
import com.pk.payment.exception.PaymentNotFoundException;
import com.pk.payment.exception.PaymentProcessingException;
import com.pk.payment.mapper.PaymentMapper;
import com.pk.payment.repository.IdempotencyRecordRepository;
import com.pk.payment.repository.OrderRepository;
import com.pk.payment.repository.PaymentRepository;
import com.pk.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse createPayment(
            String idempotencyKey,
            PaymentRequest request) {

        String requestHash = generateRequestHash(request);

        Optional<IdempotencyRecord> existingRecord =
                idempotencyRecordRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existingRecord.isPresent()) {

            IdempotencyRecord record = existingRecord.get();

            if (!record.getRequestHash().equals(requestHash)) {
                throw new IdempotencyKeyReuseException(idempotencyKey);
            }

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                return getPayment(record.getPaymentId());
            }

            throw new PaymentProcessingException(
                    "Payment is already being processed");
        }

        // Claim the key
        IdempotencyRecord record = new IdempotencyRecord();

        record.setIdempotencyKey(idempotencyKey);
        record.setRequestHash(requestHash);
        record.setStatus(IdempotencyStatus.PROCESSING);
        record.setCreatedAt(LocalDateTime.now());

        try {
            idempotencyRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException exception) {
            // Another request claimed the same key
            throw new PaymentProcessingException(
                    "Payment request is already being processed");
        }

        // Now safely process payment
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() ->
                        new OrderNotFoundException(request.orderId()));

        if (!order.canAcceptPayment()) {
            throw new InvalidOrderStateException(
                    order.getOrderId(),
                    order.getOrderStatus().name());
        }

        Payment payment = paymentMapper.toEntity(request, order);

        payment.setPaymentStatus(PaymentStatus.INITIATED);

        order.markPaymentPending();
        order.addPayment(payment);

        Payment savedPayment = paymentRepository.save(payment);

        // Mark idempotency operation completed
        record.setPaymentId(savedPayment.getPaymentId());
        record.setStatus(IdempotencyStatus.COMPLETED);

        return paymentMapper.toResponse(savedPayment);
    }

    @Override
    public PaymentResponse getPayment(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(paymentId));

        return paymentMapper.toResponse(payment);
    }

    private String generateRequestHash(PaymentRequest request) {

        String requestData = request.orderId()
                + "|" + request.paymentType();

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    requestData.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "Unable to generate request hash", exception);
        }
    }
}