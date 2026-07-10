package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.InvoiceFilterDTO;
import com.mybill.MyBill_Backend.dto.InvoicePreview;
import com.mybill.MyBill_Backend.dto.InvoiceProjection;
import com.mybill.MyBill_Backend.dto.InvoiceRequest;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.PaymentMode;
import com.mybill.MyBill_Backend.service.InvoicePdfService;
import com.mybill.MyBill_Backend.service.InvoiceService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;

    @PostMapping("/generate")
    public ResponseEntity<Invoice> generateInvoice(@Valid @RequestBody InvoiceRequest request) {
        Invoice invoice = invoiceService.generateInvoice(
                request.getClientId(),
                request.getWorkIds(),
                request.getDiscount(),
                request.getNotes(),
                request.getDueDate()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @PatchMapping("/{invoiceId}/payment")
    public ResponseEntity<Invoice> updatePayment(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody PaymentUpdateRequest request
    ) {
        Invoice updatedInvoice = invoiceService.updatePayment(
                invoiceId,
                request.getPaidAmount(),
                request.getPaymentMode(),
                request.getPaymentDate()
        );

        return ResponseEntity.ok(updatedInvoice);
    }

    @GetMapping("/pdf/{invoiceId}")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable UUID invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);

        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoiceId);

        String invoiceNumber = invoice.getInvoiceNumber() != null
                ? invoice.getInvoiceNumber()
                : "invoice-" + invoiceId;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename(invoiceNumber + ".pdf")
                        .build()
        );

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // NEW: Paginated projection response for large histories
    @GetMapping("/client/{clientId}")
    public ResponseEntity<Page<InvoiceProjection>> getInvoicesByClient(
            @PathVariable UUID clientId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.getInvoicesByClientProjected(clientId, pageable));
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<Double> getMonthlyRevenue() {
        return ResponseEntity.ok(invoiceService.getMonthlyRevenue());
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceProjection>> getInvoices(Pageable pageable) {
        return ResponseEntity.ok(invoiceService.getInvoicesProjected(pageable));
    }

    @PostMapping("/preview")
    public ResponseEntity<InvoicePreview> preview(@Valid @RequestBody InvoiceRequest request) {
        InvoicePreview preview = invoiceService.previewInvoice(
                request.getClientId(),
                request.getWorkIds()
        );

        return ResponseEntity.ok(preview);
    }

    @GetMapping("/sync")
    public ResponseEntity<List<Invoice>> getInvoicesUpdatedSince(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime since,
            org.springframework.data.domain.Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.getInvoicesUpdatedSince(since, pageable).getContent());
    }

    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable UUID invoiceId) {
        invoiceService.deleteInvoice(invoiceId);
        return ResponseEntity.noContent().build();
    }

    // NEW: Paginated projection response for searching
    @GetMapping("/search")
    public ResponseEntity<Page<InvoiceProjection>> searchInvoices(
            @RequestParam(required = false) String clientName,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.searchInvoicesProjected(clientName, month, year, pageable));
    }

    @PostMapping("/filter")
    public ResponseEntity<Page<InvoiceProjection>> filterInvoices(
            @RequestBody(required = false) InvoiceFilterDTO filter,
            Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.filterInvoices(filter, pageable));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<InvoiceProjection>> filterInvoices(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @RequestParam(required = false) List<com.mybill.MyBill_Backend.entity.PaymentStatus> statuses,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            Pageable pageable
    ) {
        InvoiceFilterDTO filter = new InvoiceFilterDTO();
        filter.setQuery(query);
        filter.setClientId(clientId);
        filter.setStartDate(startDate);
        filter.setEndDate(endDate);
        filter.setStatuses(statuses);
        filter.setMinAmount(minAmount);
        filter.setMaxAmount(maxAmount);
        return ResponseEntity.ok(invoiceService.filterInvoices(filter, pageable));
    }

    @Data
    public static class PaymentUpdateRequest {
        private String status;

        @NotNull(message = "Paid amount is required")
        @DecimalMin(value = "0.00", message = "Paid amount cannot be negative")
        @Digits(integer = 12, fraction = 2, message = "Paid amount can have at most 2 decimal places")
        private Double paidAmount;

        @NotNull(message = "Payment mode is required")
        private PaymentMode paymentMode;

        @PastOrPresent(message = "Payment date cannot be in the future")
        private LocalDateTime paymentDate;
    }
}
