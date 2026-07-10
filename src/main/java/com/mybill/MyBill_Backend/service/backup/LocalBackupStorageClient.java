package com.mybill.MyBill_Backend.service.backup;

import com.mybill.MyBill_Backend.entity.BackupJob;
import com.mybill.MyBill_Backend.entity.BackupProvider;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class LocalBackupStorageClient implements BackupStorageClient {
    @Override
    public BackupProvider provider() {
        return BackupProvider.LOCAL;
    }

    @Override
    public String store(BackupJob job, Path localBackupFile, String expectedSha256) throws Exception {
        BackupChecksum.verifySha256(localBackupFile, expectedSha256);
        return "local:" + localBackupFile.toAbsolutePath();
    }
}
