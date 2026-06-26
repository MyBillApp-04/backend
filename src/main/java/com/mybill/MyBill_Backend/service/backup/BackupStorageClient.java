package com.mybill.MyBill_Backend.service.backup;

import com.mybill.MyBill_Backend.entity.BackupJob;
import com.mybill.MyBill_Backend.entity.BackupProvider;

import java.nio.file.Path;

public interface BackupStorageClient {
    BackupProvider provider();

    String store(BackupJob job, Path localBackupFile) throws Exception;
}
