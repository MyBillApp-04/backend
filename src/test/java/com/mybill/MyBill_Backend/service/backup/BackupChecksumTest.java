package com.mybill.MyBill_Backend.service.backup;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BackupChecksumTest {

    @Test
    void computesAndVerifiesSha256() throws Exception {
        Path file = Files.createTempFile("mybill-backup-checksum-", ".json");
        try {
            Files.writeString(file, "{\"ok\":true}");

            String sha256 = BackupChecksum.sha256Hex(file);

            assertThat(sha256).matches("[a-f0-9]{64}");
            BackupChecksum.verifySha256(file, sha256);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void rejectsMismatchedSha256() throws Exception {
        Path file = Files.createTempFile("mybill-backup-checksum-", ".json");
        try {
            Files.writeString(file, "{\"ok\":false}");

            assertThatThrownBy(() -> BackupChecksum.verifySha256(
                    file,
                    "0000000000000000000000000000000000000000000000000000000000000000"
            )).isInstanceOf(BackupChecksumException.class);
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
