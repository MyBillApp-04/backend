package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.Client;
import java.time.LocalDateTime;
import java.util.UUID;

public class ClientResponse {

    private UUID id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
    private String deviceId;
    private Integer version;

    public ClientResponse(Client client) {
        this.id = client.getId();
        this.name = client.getName() != null ? client.getName() : "";
        this.phone = client.getPhone() != null ? client.getPhone() : "";
        this.email = client.getEmail() != null ? client.getEmail() : "";
        this.address = client.getAddress() != null ? client.getAddress() : "";
        this.createdAt = client.getCreatedAt();
        this.updatedAt = client.getUpdatedAt();
        this.deletedAt = client.getDeletedAt();
        this.isDeleted = client.getIsDeleted();
        this.deviceId = client.getDeviceId();
        this.version = client.getVersion();
    }

    // NEW: Constructor to map directly from projection avoiding entity overhead
    public ClientResponse(ClientProjection projection) {
        this.id = projection.getId();
        this.name = projection.getName() != null ? projection.getName() : "";
        this.phone = projection.getPhone() != null ? projection.getPhone() : "";
        this.email = projection.getEmail() != null ? projection.getEmail() : "";
        this.address = projection.getAddress() != null ? projection.getAddress() : "";
        this.createdAt = projection.getCreatedAt();
        this.updatedAt = projection.getUpdatedAt();
        this.deletedAt = projection.getDeletedAt();
        this.isDeleted = projection.getIsDeleted();
        this.deviceId = projection.getDeviceId();
        this.version = projection.getVersion();
    }

    // Getters and Setters...
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
    public Boolean getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Boolean isDeleted) { this.isDeleted = isDeleted; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
