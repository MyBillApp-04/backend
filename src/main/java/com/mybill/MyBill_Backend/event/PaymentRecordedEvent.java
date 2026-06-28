package com.mybill.MyBill_Backend.event;

import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.Payment;
import org.springframework.context.ApplicationEvent;

public class PaymentRecordedEvent extends ApplicationEvent {
    private final Payment payment;
    private final Invoice invoice;
    private final double amount;
    private final double remainingAmount;

    public PaymentRecordedEvent(Object source, Payment payment, Invoice invoice, double amount, double remainingAmount) {
        super(source);
        this.payment = payment;
        this.invoice = invoice;
        this.amount = amount;
        this.remainingAmount = remainingAmount;
    }

    public Payment getPayment() {
        return payment;
    }

    public Invoice getInvoice() {
        return invoice;
    }

    public double getAmount() {
        return amount;
    }

    public double getRemainingAmount() {
        return remainingAmount;
    }
}
