package com.mybill.MyBill_Backend.dto;

import com.mybill.MyBill_Backend.entity.BackupProvider;
import lombok.Data;

@Data
public class BackupRequest {
    private BackupProvider provider;
}
