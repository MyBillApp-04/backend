package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.ClientFinancialSummaryDTO;
import com.mybill.MyBill_Backend.dto.ClientLedgerEntryDTO;
import com.mybill.MyBill_Backend.dto.ReceivePaymentRequest;
import com.mybill.MyBill_Backend.dto.ReceivePaymentResponse;
import com.mybill.MyBill_Backend.service.ClientFinancialService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clients/{clientId}/financial")
@RequiredArgsConstructor
public class ClientFinancialController {

    private final ClientFinancialService financialService;

    @GetMapping("/summary")
    public ResponseEntity<ClientFinancialSummaryDTO> getSummary(@PathVariable UUID clientId) {
        return ResponseEntity.ok(financialService.getSummary(clientId));
    }

    @GetMapping("/ledger")
    public ResponseEntity<Page<ClientLedgerEntryDTO>> getLedger(@PathVariable UUID clientId, Pageable pageable) {
        return ResponseEntity.ok(financialService.getLedger(clientId, pageable).map(ClientLedgerEntryDTO::fromEntity));
    }

    @PostMapping("/payments")
    public ResponseEntity<ReceivePaymentResponse> receivePayment(
            @PathVariable UUID clientId,
            @Valid @RequestBody ReceivePaymentRequest request
    ) {
        return ResponseEntity.ok(financialService.receivePayment(clientId, request));
    }
}
