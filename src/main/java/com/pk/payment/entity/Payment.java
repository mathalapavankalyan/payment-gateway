package com.pk.payment.entity;

import java.math.BigDecimal;

import com.pk.payment.enums.PaymentStatus;
import com.pk.payment.enums.PaymentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
public class Payment {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long paymentId;
	
	@Enumerated(EnumType.STRING)
	private PaymentType paymentType;
	@Enumerated(EnumType.STRING)
	private PaymentStatus paymentStatus;
	@Column(length = 3)
	private String currency;	
	private BigDecimal amount;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id" , nullable = false)
	private Order order;

	
	
	public void markProcessing() {
	    this.paymentStatus = PaymentStatus.PROCESSING;
	}

	public void markSuccessful() {
	    this.paymentStatus = PaymentStatus.SUCCESS;
	}

	public void markFailed() {
	    this.paymentStatus = PaymentStatus.FAILED;
	}
}
