package com.mybill.MyBill_Backend.service.backup;

import com.mybill.MyBill_Backend.entity.BackupJob;
import com.mybill.MyBill_Backend.entity.BackupProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class S3BackupStorageClient implements BackupStorageClient {

    @Value("${app.backup.s3.bucket:}")
    private String bucket;

    @Value("${app.backup.s3.prefix:mybill-backups}")
    private String prefix;

    @Value("${app.backup.s3.region:ap-south-1}")
    private String region;

    @Override
    public BackupProvider provider() {
        return BackupProvider.AWS_S3;
    }

    @Override
    public String store(BackupJob job, Path localBackupFile, String expectedSha256) throws Exception {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("AWS S3 backup bucket is not configured");
        }

        String key = prefix + "/" + job.getUser().getId() + "/" + localBackupFile.getFileName();
        try (S3Client s3 = S3Client.builder().region(Region.of(region)).build()) {
            BackupChecksum.verifySha256(localBackupFile, expectedSha256);

            s3.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromFile(localBackupFile)
            );

            Path downloadedCopy = Files.createTempFile("mybill-s3-backup-verify-", ".json");
            try {
                s3.getObject(
                        GetObjectRequest.builder()
                                .bucket(bucket)
                                .key(key)
                                .build(),
                        downloadedCopy
                );
                BackupChecksum.verifySha256(downloadedCopy, expectedSha256);
            } finally {
                Files.deleteIfExists(downloadedCopy);
            }
        }

        return "s3://" + bucket + "/" + key;
    }
}
