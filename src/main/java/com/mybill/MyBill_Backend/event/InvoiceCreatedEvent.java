package com.mybill.MyBill_Backend.event;

import com.mybill.MyBill_Backend.entity.Invoice;
import org.springframework.context.ApplicationEvent;

public class InvoiceCreatedEvent extends ApplicationEvent {
    private final Invoice invoice;

    public InvoiceCreatedEvent(Object source, Invoice invoice) {
        super(source);
        this.invoice = invoice;
    }

    public Invoice getInvoice() {
        return invoice;
    }
}
