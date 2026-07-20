package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.QuotationDTO;
import com.mybill.MyBill_Backend.dto.QuotationItemDTO;
import com.mybill.MyBill_Backend.entity.Quotation;
import com.mybill.MyBill_Backend.entity.QuotationItem;
import com.mybill.MyBill_Backend.service.QuotationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/quotations")
@RequiredArgsConstructor
public class QuotationController {

    private final QuotationService quotationService;

    @GetMapping
    public ResponseEntity<Page<QuotationDTO>> getQuotations(Pageable pageable) {
        Page<QuotationDTO> page = quotationService.getQuotationsForUser(pageable).map(this::toDTO);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuotationDTO> getQuotation(@PathVariable UUID id) {
        Quotation quotation = quotationService.getQuotationById(id);
        return ResponseEntity.ok(toDTO(quotation));
    }

    @PostMapping
    public ResponseEntity<QuotationDTO> createQuotation(@Valid @RequestBody QuotationDTO dto) {
        Quotation created = quotationService.createQuotation(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuotationDTO> updateQuotation(
            @PathVariable UUID id,
            @Valid @RequestBody QuotationDTO dto
    ) {
        Quotation updated = quotationService.updateQuotation(id, dto);
        return ResponseEntity.ok(toDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuotation(@PathVariable UUID id) {
        quotationService.deleteQuotation(id);
        return ResponseEntity.noContent().build();
    }

    private QuotationDTO toDTO(Quotation q) {
        List<QuotationItemDTO> items = q.getItems() == null ? List.of() : q.getItems().stream()
                .filter(item -> !Boolean.TRUE.equals(item.getIsDeleted()))
                .map(item -> QuotationItemDTO.builder()
                        .id(item.getId())
                        .description(item.getDescription())
                        .dimension(item.getDimension())
                        .quantity(item.getQuantity())
                        .kgs(item.getKgs())
                        .amount(item.getAmount())
                        .version(item.getVersion())
                        .build())
                .collect(Collectors.toList());

        return QuotationDTO.builder()
                .id(q.getId())
                .clientId(q.getClient() != null ? q.getClient().getId() : null)
                .clientName(q.getClient() != null ? q.getClient().getName() : null)
                .quotationNumber(q.getQuotationNumber())
                .status(q.getStatus())
                .issueDate(q.getIssueDate())
                .validUntilDate(q.getValidUntilDate())
                .notes(q.getNotes())
                .termsAndConditions(q.getTermsAndConditions())
                .pdfUrl(q.getPdfUrl())
                .pdfPath(q.getPdfPath())
                .subtotal(q.getSubtotal())
                .discount(q.getDiscount())
                .grossAmount(q.getGrossAmount())
                .totalAmount(q.getTotalAmount())
                .netPayable(q.getNetPayable())
                .version(q.getVersion())
                .items(items)
                .build();
    }
}
