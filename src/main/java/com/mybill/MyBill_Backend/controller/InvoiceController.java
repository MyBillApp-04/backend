package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.InvoiceFilterDTO;
import com.mybill.MyBill_Backend.dto.InvoicePreview;
import com.mybill.MyBill_Backend.dto.InvoiceProjection;
import com.mybill.MyBill_Backend.dto.InvoiceRequest;
import com.mybill.MyBill_Backend.entity.Invoice;
import com.mybill.MyBill_Backend.entity.PaymentMode;
import com.mybill.MyBill_Backend.service.InvoicePdfService;
import com.mybill.MyBill_Backend.service.InvoiceService;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoice")
@RequiredArgsConstructor
@Validated
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoicePdfService invoicePdfService;
    private final SecurityUtils securityUtils;

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
    public ResponseEntity<StreamingResponseBody> downloadInvoice(@PathVariable UUID invoiceId) {
        Invoice invoice = invoiceService.getInvoiceById(invoiceId);

        String invoiceNumber = invoice.getInvoiceNumber() != null
                ? invoice.getInvoiceNumber()
                : "invoice-" + invoiceId;
        Long userId = securityUtils.getCurrentUserId();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(
                ContentDisposition.builder("attachment")
                        .filename(invoiceNumber + ".pdf")
                        .build()
        );

        StreamingResponseBody stream = outputStream -> invoicePdfService.generateInvoicePdf(invoiceId, outputStream, userId);
        return new ResponseEntity<>(stream, headers, HttpStatus.OK);
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
            @RequestParam(required = false)
            @Size(max = 120, message = "Client name search must be 120 characters or fewer")
            String clientName,
            @RequestParam(required = false)
            @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12")
            Integer month,
            @RequestParam(required = false)
            @Min(value = 2000, message = "Year must be 2000 or later")
            @Max(value = 2100, message = "Year must be 2100 or earlier")
            Integer year,
            Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.searchInvoicesProjected(clientName, month, year, pageable));
    }

    @PostMapping("/filter")
    public ResponseEntity<Page<InvoiceProjection>> filterInvoices(
            @Valid @RequestBody(required = false) InvoiceFilterDTO filter,
            Pageable pageable
    ) {
        return ResponseEntity.ok(invoiceService.filterInvoices(filter, pageable));
    }

    @GetMapping("/filter")
    public ResponseEntity<Page<InvoiceProjection>> filterInvoices(
            @RequestParam(required = false)
            @Size(max = 120, message = "Search query must be 120 characters or fewer")
            String query,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime endDate,
            @RequestParam(required = false) List<com.mybill.MyBill_Backend.entity.PaymentStatus> statuses,
            @RequestParam(required = false)
            @DecimalMin(value = "0.00", message = "Minimum amount cannot be negative")
            @Digits(integer = 12, fraction = 2, message = "Minimum amount can have at most 2 decimal places")
            Double minAmount,
            @RequestParam(required = false)
            @DecimalMin(value = "0.00", message = "Maximum amount cannot be negative")
            @Digits(integer = 12, fraction = 2, message = "Maximum amount can have at most 2 decimal places")
            Double maxAmount,
            Pageable pageable
    ) {
        validateFilterRanges(startDate, endDate, minAmount, maxAmount);

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

    private void validateFilterRanges(
            LocalDateTime startDate,
            LocalDateTime endDate,
            Double minAmount,
            Double maxAmount
    ) {
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date must be on or after start date");
        }
        if (minAmount != null && maxAmount != null && maxAmount < minAmount) {
            throw new IllegalArgumentException("Maximum amount must be greater than or equal to minimum amount");
        }
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
