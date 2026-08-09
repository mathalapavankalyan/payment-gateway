package com.pk.payment.service;

import com.pk.payment.dto.PaymentRequest;
import com.pk.payment.dto.PaymentResponse;

public interface PaymentService {
	
	 PaymentResponse createPayment(String idempotencyKey, PaymentRequest payment);
	 
	 PaymentResponse getPayment(Long paymentId);

}
