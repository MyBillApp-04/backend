package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.GlobalSearchResponse;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.entity.ClientWork;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.Quotation;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.repository.InvoiceRepository;
import com.mybill.MyBill_Backend.repository.QuotationRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final QuotationRepository quotationRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public GlobalSearchResponse globalSearch(String query, Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        String safeQuery = query == null ? "" : query.trim();
        Pageable boundedPageable = boundedSearchPageable(pageable);

        return GlobalSearchResponse.builder()
                .clients(clientRepository.searchProjectedByUserIdAndQuery(userId, safeQuery, boundedPageable)
                        .map(client -> {
                            Map<String, Object> m = new LinkedHashMap<>();
                            m.put("id", client.getId());
                            m.put("name", client.getName());
                            m.put("phone", client.getPhone());
                            return m;
                        })
                        .toList())
                .works(workRepository.searchByUserIdAndQuery(userId, safeQuery, boundedPageable)
                        .map(this::workToMap)
                        .toList())
                .invoices(invoiceRepository.searchInvoices(userId, safeQuery, null, null, boundedPageable)
                        .map(this::invoiceToMap)
                        .toList())
                .quotations(quotationRepository.searchQuotations(userId, safeQuery, boundedPageable)
                        .map(this::quotationToMap)
                        .toList())
                .build();
    }

    private Pageable boundedSearchPageable(Pageable pageable) {
        if (pageable == null || pageable.isUnpaged()) {
            return PageRequest.of(0, 8);
        }

        int size = Math.min(pageable.getPageSize(), 25);
        return PageRequest.of(pageable.getPageNumber(), size, pageable.getSort());
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

    private Map<String, Object> quotationToMap(Quotation quotation) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", quotation.getId());
        m.put("quotationNumber", quotation.getQuotationNumber());
        m.put("totalAmount", quotation.getTotalAmount());
        m.put("status", quotation.getStatus());
        m.put("clientName", quotation.getClient() != null ? quotation.getClient().getName() : null);
        return m;
    }
}
