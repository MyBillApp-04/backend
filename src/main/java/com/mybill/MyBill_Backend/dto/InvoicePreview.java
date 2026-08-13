package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.TaxType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InvoicePreview {
    private Client client;
    private List<ClientWork> works;
    private double total;

    // GST preview snapshot
    private double taxRate;
    private TaxType gstType;
    private double taxableAmount;
    private double taxAmount;
    private double cgstAmount;
    private double sgstAmount;
    private double igstAmount;
}