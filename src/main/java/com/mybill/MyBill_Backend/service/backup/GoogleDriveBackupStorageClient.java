package com.mybill.MyBill_Backend.service.backup;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybill.MyBill_Backend.entity.BackupJob;
import com.mybill.MyBill_Backend.entity.BackupProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class GoogleDriveBackupStorageClient implements BackupStorageClient {

    @Value("${app.backup.google-drive.access-token:}")
    private String accessToken;

    @Value("${app.backup.google-drive.folder-id:}")
    private String folderId;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public BackupProvider provider() {
        return BackupProvider.GOOGLE_DRIVE;
    }

    @Override
    @CircuitBreaker(name = "googleDriveService", fallbackMethod = "fallbackStore")
    public String store(BackupJob job, Path localBackupFile, String expectedSha256) throws Exception {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Google Drive access token is not configured");
        }

        BackupChecksum.verifySha256(localBackupFile, expectedSha256);

        String boundary = "mybill-backup-" + job.getBackupId();
        String metadata = "{\"name\":\"" + localBackupFile.getFileName() + "\""
                + (folderId == null || folderId.isBlank() ? "" : ",\"parents\":[\"" + folderId + "\"]")
                + "}";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(multipartBody(boundary, metadata, localBackupFile))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Google Drive backup upload failed: HTTP " + response.statusCode());
        }

        String fileId = extractFileId(response.body());
        verifyUploadedFile(fileId, expectedSha256);

        return "google-drive:" + response.body();
    }

    public String fallbackStore(
            BackupJob job,
            Path localBackupFile,
            String expectedSha256,
            Throwable t
    ) throws Exception {
        throw new RuntimeException("Google Drive service unavailable (Circuit Breaker). Reason: " + t.getMessage(), t);
    }

    private HttpRequest.BodyPublisher multipartBody(String boundary, String metadata, Path file) throws IOException {
        String head = "--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadata + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: application/json\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";

        return HttpRequest.BodyPublishers.concat(
                HttpRequest.BodyPublishers.ofString(head),
                HttpRequest.BodyPublishers.ofFile(file),
                HttpRequest.BodyPublishers.ofString(tail)
        );
    }

    private String extractFileId(String responseBody) throws IOException {
        JsonNode json = objectMapper.readTree(responseBody);
        JsonNode id = json.get("id");
        if (id == null || id.asText().isBlank()) {
            throw new IOException("Google Drive backup upload response did not include a file id");
        }
        return id.asText();
    }

    private void verifyUploadedFile(String fileId, String expectedSha256) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/drive/v3/files/" + fileId + "?alt=media"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        Path downloadedCopy = Files.createTempFile("mybill-google-drive-backup-verify-", ".json");
        try {
            HttpResponse<Path> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofFile(downloadedCopy)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Google Drive backup verification download failed: HTTP "
                        + response.statusCode());
            }
            BackupChecksum.verifySha256(downloadedCopy, expectedSha256);
        } finally {
            Files.deleteIfExists(downloadedCopy);
        }
    }
}
