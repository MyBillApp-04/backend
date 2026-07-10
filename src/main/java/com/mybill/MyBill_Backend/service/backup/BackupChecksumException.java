package com.mybill.MyBill_Backend.service.backup;

import java.io.IOException;

public class BackupChecksumException extends IOException {
    public BackupChecksumException(String message) {
        super(message);
    }
}
