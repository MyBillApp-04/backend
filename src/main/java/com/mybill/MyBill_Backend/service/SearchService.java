package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.GlobalSearchResponse;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ClientRepository clientRepository;
    private final ClientWorkRepository workRepository;
    private final InvoiceRepository invoiceRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public GlobalSearchResponse globalSearch(String query, Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        String safeQuery = query == null ? "" : query.trim();

        return GlobalSearchResponse.builder()
                .clients(clientRepository.searchProjectedByUserIdAndQuery(userId, safeQuery, pageable)
                        .map(client -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("id", client.getId());
                            m.put("name", client.getName());
                            m.put("phone", client.getPhone());
                            return m;
                        })
                        .toList())
                .works(workRepository.searchByUserIdAndQuery(userId, safeQuery, pageable)
                        .map(this::workToMap)
                        .toList())
                .invoices(invoiceRepository.searchInvoices(userId, safeQuery, null, null)
                        .stream()
                        .limit(pageable.getPageSize())
                        .map(this::invoiceToMap)
                        .toList())
                .build();
    }

    private Map<String, Object> workToMap(ClientWork work) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", work.getId());
        m.put("description", work.getDescription());
        m.put("amount", work.getAmount());
        m.put("clientName", work.getClient() != null ? work.getClient().getName() : null);
        return m;
    }

    private Map<String, Object> invoiceToMap(Invoice invoice) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", invoice.getId());
        m.put("invoiceNumber", invoice.getInvoiceNumber());
        m.put("totalAmount", invoice.getTotalAmount());
        m.put("paidAmount", invoice.getPaidAmount());
        m.put("remainingAmount", invoice.getRemainingAmount());
        m.put("paymentStatus", invoice.getPaymentStatus());
        m.put("clientName", invoice.getClient() != null ? invoice.getClient().getName() : null);
        return m;
    }
}
