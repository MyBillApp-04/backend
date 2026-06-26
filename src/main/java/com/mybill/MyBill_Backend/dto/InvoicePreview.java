package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class InvoicePreview {
    private Client client;
    private List<ClientWork> works;
    private double total;
}
