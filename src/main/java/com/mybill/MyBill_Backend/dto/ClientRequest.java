package com.mybill.MyBill_Backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class ClientRequest {

    private UUID id;

    @NotBlank(message = "Client name is required")
    @Size(max = 120, message = "Client name must be 120 characters or fewer")
    @Pattern(regexp = "^[\\p{L}\\p{N} .,'&()/-]+$", message = "Client name can only contain letters, numbers, spaces, and basic punctuation")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^(?:\\+91[- ]?)?[6-9]\\d{9}$", message = "Phone number must be a valid Indian mobile number, for example 9876543210 or +91 9876543210")
    private String phone;

    @Email(message = "Email must be a valid address")
    @Pattern(
            regexp = "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$",
            message = "Email must include a valid domain, for example name@example.com"
    )
    @Size(max = 254, message = "Email must be 254 characters or fewer")
    private String email;

    @Size(max = 500, message = "Address must be 500 characters or fewer")
    private String address;

    @Size(max = 120, message = "Device ID must be 120 characters or fewer")
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
