package com.mybill.MyBill_Backend.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class GlobalSearchResponse {
    private List<Map<String, Object>> clients;
    private List<Map<String, Object>> works;
    private List<Map<String, Object>> invoices;
    private List<Map<String, Object>> quotations;

    public GlobalSearchResponse() {}

    public GlobalSearchResponse(List<Map<String, Object>> clients, List<Map<String, Object>> works, List<Map<String, Object>> invoices, List<Map<String, Object>> quotations) {
        this.clients = clients;
        this.works = works;
        this.invoices = invoices;
        this.quotations = quotations;
    }

    public static GlobalSearchResponseBuilder builder() { return new GlobalSearchResponseBuilder(); }

    public static class GlobalSearchResponseBuilder {
        private List<Map<String, Object>> clients;
        private List<Map<String, Object>> works;
        private List<Map<String, Object>> invoices;
        private List<Map<String, Object>> quotations;

        public GlobalSearchResponseBuilder clients(List<Map<String, Object>> clients) { this.clients = clients; return this; }
        public GlobalSearchResponseBuilder works(List<Map<String, Object>> works) { this.works = works; return this; }
        public GlobalSearchResponseBuilder invoices(List<Map<String, Object>> invoices) { this.invoices = invoices; return this; }
        public GlobalSearchResponseBuilder quotations(List<Map<String, Object>> quotations) { this.quotations = quotations; return this; }

        public GlobalSearchResponse build() {
            return new GlobalSearchResponse(clients, works, invoices, quotations);
        }
    }

    public List<Map<String, Object>> getClients() { return clients; }
    public void setClients(List<Map<String, Object>> clients) { this.clients = clients; }

    public List<Map<String, Object>> getWorks() { return works; }
    public void setWorks(List<Map<String, Object>> works) { this.works = works; }

    public List<Map<String, Object>> getInvoices() { return invoices; }
    public void setInvoices(List<Map<String, Object>> invoices) { this.invoices = invoices; }

    public List<Map<String, Object>> getQuotations() { return quotations; }
    public void setQuotations(List<Map<String, Object>> quotations) { this.quotations = quotations; }
}
