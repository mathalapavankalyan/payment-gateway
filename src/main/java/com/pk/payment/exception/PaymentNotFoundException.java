package com.pk.payment.exception;

@SuppressWarnings("serial")
public class PaymentNotFoundException extends RuntimeException {

	
	public PaymentNotFoundException(Long paymentId) {
		 super("Payment not found with id: " + paymentId);
	}
}
