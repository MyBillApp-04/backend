package com.mybill.MyBill_Backend.controller;

import com.mybill.MyBill_Backend.dto.ClientRequest;
import com.mybill.MyBill_Backend.dto.ClientResponse;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.service.ClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(
            @RequestBody ClientRequest request
    ) {
        Client client = toEntity(request);

        Client saved = clientService.createClient(client);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ClientResponse(saved));
    }

    // NEW: Full pagination using Page<ClientResponse> mapped directly from projections
    @GetMapping
    public ResponseEntity<Page<ClientResponse>> getAllClients(Pageable pageable) {
        return ResponseEntity.ok(
                clientService.getAllClients(pageable).map(ClientResponse::new)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable UUID id) {
        Client client = clientService.getClientById(id);

        return ResponseEntity.ok(new ClientResponse(client));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable UUID id,
            @RequestBody ClientRequest request
    ) {
        Client patch = toEntity(request);

        Client updated = clientService.updateClient(id, patch);

        return ResponseEntity.ok(new ClientResponse(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable UUID id) {
        clientService.deleteClient(id);

        return ResponseEntity.noContent().build();
    }

    // NEW: Full pagination support for searches
    @GetMapping("/search")
    public ResponseEntity<Page<ClientResponse>> searchClients(
            @RequestParam String query,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                clientService.searchClients(query, pageable).map(ClientResponse::new)
        );
    }

    @GetMapping("/sync")
    public List<ClientResponse> getClientsUpdatedSince(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime since
    ) {
        return clientService.getClientsUpdatedSince(since)
                .stream()
                .map(ClientResponse::new)
                .toList();
    }

    private Client toEntity(ClientRequest request) {
        Client client = new Client();

        client.setId(request.getId());
        client.setName(request.getName());
        client.setPhone(request.getPhone());
        client.setEmail(request.getEmail());
        client.setAddress(request.getAddress());
        client.setDeviceId(request.getDeviceId());

        return client;
    }
}
