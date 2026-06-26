package com.mybill.MyBill_Backend.service;

import com.mybill.MyBill_Backend.dto.ClientProjection;
import com.mybill.MyBill_Backend.entity.Client;
import com.mybill.MyBill_Backend.repository.ClientRepository;
import com.mybill.MyBill_Backend.repository.ClientWorkRepository;
import com.mybill.MyBill_Backend.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService {

    private final ClientRepository clientRepository;
    private final ClientWorkRepository workRepository;
    private final SecurityUtils securityUtils;

    public Client createClient(Client client) {
        if (client.getId() == null) {
            client.setId(UUID.randomUUID());
        }

        LocalDateTime now = LocalDateTime.now();

        client.setUser(securityUtils.getCurrentUser());
        client.setName(upper(client.getName()));
        client.setCreatedAt(client.getCreatedAt() != null ? client.getCreatedAt() : now);
        client.setUpdatedAt(client.getUpdatedAt() != null ? client.getUpdatedAt() : now);
        client.setIsDeleted(client.getIsDeleted() != null ? client.getIsDeleted() : false);
        client.setVersion(client.getVersion() != null ? client.getVersion() : 1);

        return clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public List<Client> getAllClients() {
        return clientRepository.findByUserIdAndIsDeletedFalse(securityUtils.getCurrentUserId());
    }

    // NEW: Paginated projection fetch
    @Transactional(readOnly = true)
    public Page<ClientProjection> getAllClients(Pageable pageable) {
        return clientRepository.findProjectedByUserIdAndIsDeletedFalse(
                securityUtils.getCurrentUserId(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public Client getClientById(UUID id) {
        Long currentUserId = securityUtils.getCurrentUserId();

        return clientRepository.findByIdAndUserId(id, currentUserId)
                .orElseThrow(() -> new AccessDeniedException("Client not found or access denied"));
    }

    public Client updateClient(UUID id, Client updatedClient) {
        Client client = getClientById(id);

        client.setName(upper(updatedClient.getName()));
        client.setPhone(updatedClient.getPhone());
        client.setEmail(updatedClient.getEmail());
        client.setAddress(updatedClient.getAddress());
        client.setDeviceId(updatedClient.getDeviceId());

        if (updatedClient.getUpdatedAt() != null &&
                updatedClient.getUpdatedAt().isAfter(client.getUpdatedAt())) {
            client.setUpdatedAt(updatedClient.getUpdatedAt());
        }

        return clientRepository.save(client);
    }

    public void deleteClient(UUID id) {
        Client client = getClientById(id);
        client.markDeleted(LocalDateTime.now());

        clientRepository.save(client);
    }

    @Transactional(readOnly = true)
    public List<Client> searchClients(String query) {
        return clientRepository.searchByUserIdAndQuery(securityUtils.getCurrentUserId(), query);
    }

    // NEW: Paginated projection search
    @Transactional(readOnly = true)
    public Page<ClientProjection> searchClients(String query, Pageable pageable) {
        return clientRepository.searchProjectedByUserIdAndQuery(
                securityUtils.getCurrentUserId(),
                query,
                pageable
        );
    }

    @Transactional(readOnly = true)
    public List<Client> getClientsUpdatedSince(LocalDateTime since) {
        return clientRepository.findByUserIdAndUpdatedAtAfter(
                securityUtils.getCurrentUserId(),
                since
        );
    }

    private String upper(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}
