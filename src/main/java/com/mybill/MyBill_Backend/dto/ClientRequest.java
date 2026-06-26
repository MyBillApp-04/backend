package com.mybill.MyBill_Backend.dto;

import java.util.UUID;

public class ClientRequest {

    private UUID id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String deviceId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return clean(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return clean(phone);
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return clean(email);
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return clean(address);
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDeviceId() {
        return clean(deviceId);
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    private String clean(String value) {
        return value != null ? value.trim() : null;
    }
}
