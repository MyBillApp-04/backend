package com.mybill.MyBill_Backend.service.backup;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class BackupChecksum {

    private BackupChecksum() {
    }

    public static String sha256Hex(Path file) throws IOException {
        MessageDigest digest = sha256Digest();

        try (InputStream input = Files.newInputStream(file);
             DigestInputStream digestInput = new DigestInputStream(input, digest)) {
            digestInput.transferTo(OutputStreamSink.INSTANCE);
        }

        return HexFormat.of().formatHex(digest.digest());
    }

    public static void verifySha256(Path file, String expectedSha256) throws IOException {
        String actualSha256 = sha256Hex(file);
        if (!actualSha256.equalsIgnoreCase(expectedSha256)) {
            throw new BackupChecksumException(
                    "Backup checksum verification failed: expected "
                            + expectedSha256 + " but got " + actualSha256
            );
        }
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 digest is not available", exception);
        }
    }

    private static final class OutputStreamSink extends java.io.OutputStream {
        private static final OutputStreamSink INSTANCE = new OutputStreamSink();

        @Override
        public void write(int b) {
        }

        @Override
        public void write(byte[] b, int off, int len) {
        }
    }
}
