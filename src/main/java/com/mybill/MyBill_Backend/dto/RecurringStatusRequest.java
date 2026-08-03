package com.mybill.MyBill_Backend.dto;
import jakarta.validation.constraints.NotNull;
public record RecurringStatusRequest(@NotNull Status status) {
    public enum Status { ACTIVE, PAUSED, CANCELLED }
}
