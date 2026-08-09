package com.pk.payment.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.pk.payment.enums.OrderStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "orders")
public class Order {
   
   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   private Long orderId;
   
   private Integer quantity;
   
   @Enumerated(EnumType.STRING)
   private OrderStatus orderStatus;
   @Column(length = 3)
   private String currency;
   private BigDecimal amount;
   
   @OneToMany(mappedBy = "order")
   private List<Payment> payments = new ArrayList<>();
   
   public void addPayment(Payment payment) {
	    payments.add(payment);
	    payment.setOrder(this);
	}
   
   public void markPaymentPending() {
	    this.orderStatus = OrderStatus.PAYMENT_PENDING;
	}

	public void markPaid() {
	    this.orderStatus = OrderStatus.PAID;
	}

	public void cancel() {
	    this.orderStatus = OrderStatus.CANCELLED;
	}
	
	
	public boolean canAcceptPayment() {
	    return orderStatus == OrderStatus.CREATED
	            || orderStatus == OrderStatus.PAYMENT_PENDING;
	}
   
}
