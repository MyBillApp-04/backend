package com.mybill.MyBill_Backend.service.backup;

import com.mybill.MyBill_Backend.entity.BackupJob;
import com.mybill.MyBill_Backend.entity.BackupProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class GoogleDriveBackupStorageClient implements BackupStorageClient {

    @Value("${app.backup.google-drive.access-token:}")
    private String accessToken;

    @Value("${app.backup.google-drive.folder-id:}")
    private String folderId;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Override
    public BackupProvider provider() {
        return BackupProvider.GOOGLE_DRIVE;
    }

    @Override
    public String store(BackupJob job, Path localBackupFile) throws Exception {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalStateException("Google Drive access token is not configured");
        }

        String boundary = "mybill-backup-" + job.getBackupId();
        String metadata = "{\"name\":\"" + localBackupFile.getFileName() + "\""
                + (folderId == null || folderId.isBlank() ? "" : ",\"parents\":[\"" + folderId + "\"]")
                + "}";

        byte[] body = multipartBody(boundary, metadata, localBackupFile);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "multipart/related; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Google Drive backup upload failed: HTTP " + response.statusCode());
        }

        return "google-drive:" + response.body();
    }

    private byte[] multipartBody(String boundary, String metadata, Path file) throws IOException {
        byte[] json = Files.readAllBytes(file);
        String head = "--" + boundary + "\r\n"
                + "Content-Type: application/json; charset=UTF-8\r\n\r\n"
                + metadata + "\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Type: application/json\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";

        byte[] headBytes = head.getBytes(StandardCharsets.UTF_8);
        byte[] tailBytes = tail.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headBytes.length + json.length + tailBytes.length];
        System.arraycopy(headBytes, 0, body, 0, headBytes.length);
        System.arraycopy(json, 0, body, headBytes.length, json.length);
        System.arraycopy(tailBytes, 0, body, headBytes.length + json.length, tailBytes.length);
        return body;
    }
}
