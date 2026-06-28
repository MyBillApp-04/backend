package com.mybill.MyBill_Backend.event;

import com.mybill.MyBill_Backend.entity.Invoice;
import org.springframework.context.ApplicationEvent;

public class InvoiceUpdatedEvent extends ApplicationEvent {
    private final Invoice invoice;

    public InvoiceUpdatedEvent(Object source, Invoice invoice) {
        super(source);
        this.invoice = invoice;
    }

    public Invoice getInvoice() {
        return invoice;
    }
}
