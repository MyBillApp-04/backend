package com.mybill.MyBill_Backend;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationPreprocessor {

    private static boolean processed = false;

    public static synchronized void process() {
        if (processed) return;
        processed = true;

        try {
            // Find target directory for db migrations
            Path migrationDir = Paths.get("target/classes/db/migration");
            if (!Files.exists(migrationDir)) {
                return;
            }

            File[] files = migrationDir.toFile().listFiles((dir, name) -> name.endsWith(".sql"));
            if (files == null) return;

            Pattern alterTablePattern = Pattern.compile("(?i)^\\s*ALTER\\s+TABLE\\s+([\\w\\.]+)");

            for (File file : files) {
                // Read complete file content
                String fileContent = new String(Files.readAllBytes(file.toPath()), "UTF-8");

                // Strip out Postgres DO blocks
                fileContent = fileContent.replaceAll("(?s)DO\\s+\\$\\$.*?END\\s+\\$\\$;?", "-- Removed DO block for H2 compatibility");

                // Strip out Postgres CREATE INDEX statements
                fileContent = fileContent.replaceAll("(?s)CREATE\\s+(UNIQUE\\s+)?INDEX\\s+.*?;", "-- Removed INDEX statement for H2 compatibility");

                // Strip out Postgres custom function definitions
                fileContent = fileContent.replaceAll("(?s)CREATE\\s+(OR\\s+REPLACE\\s+)?FUNCTION\\s+.*?LANGUAGE\\s+plpgsql;?", "-- Removed FUNCTION statement for H2 compatibility");

                // H2 does not support PostgreSQL's DROP/CREATE TRIGGER ... ON syntax.
                fileContent = fileContent.replaceAll("(?is)DROP\\s+TRIGGER\\s+IF\\s+EXISTS\\s+.*?;", "-- Removed TRIGGER statement for H2 compatibility");
                fileContent = fileContent.replaceAll("(?is)CREATE\\s+TRIGGER\\s+.*?EXECUTE\\s+FUNCTION\\s+.*?;", "-- Removed TRIGGER statement for H2 compatibility");

                // Split the content into lines
                String[] rawLines = fileContent.split("\\r?\\n");
                
                // Join multiline statement continuations (lines that don't end with comma or semicolon)
                List<String> lines = new ArrayList<>();
                String pendingLine = null;
                for (String line : rawLines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--") || trimmed.startsWith("//")) {
                        if (pendingLine != null) {
                            lines.add(pendingLine);
                            pendingLine = null;
                        }
                        lines.add(line);
                        continue;
                    }

                    if (pendingLine == null) {
                        pendingLine = line;
                    } else {
                        String pendingTrimmed = pendingLine.trim();
                        if (pendingTrimmed.endsWith(",") || pendingTrimmed.endsWith(";")) {
                            lines.add(pendingLine);
                            pendingLine = line;
                        } else {
                            pendingLine = pendingLine + " " + trimmed;
                        }
                    }
                }
                if (pendingLine != null) {
                    lines.add(pendingLine);
                }

                List<String> processedLines = new ArrayList<>();
                String currentTable = null;
                boolean inAlterBlock = false;

                for (String line : lines) {
                    line = line.replaceAll("(?i)gen_random_uuid\\(\\)", "random_uuid()")
                               .replaceAll("(?i)uuid_generate_v4\\(\\)", "random_uuid()")
                               .replaceAll("(?i)CREATE\\s+TABLE\\s+(?!IF\\s+NOT\\s+EXISTS)", "CREATE TABLE IF NOT EXISTS ")
                               .replaceAll("(?i)PRIMARY\\s+KEY\\s+DEFAULT\\s+random_uuid\\(\\)", "DEFAULT random_uuid() PRIMARY KEY")
                               .replaceAll("(?i)^\\s*CREATE\\s+EXTENSION.*", "-- Removed CREATE EXTENSION statement")
                               .replaceAll("(?i)public\\.email_templates\\s*\\(\\s*template_type,\\s*subject,\\s*html_body,\\s*is_deleted\\s*\\)", "public.email_templates(template_id, template_type, subject, html_body, is_deleted)")
                               .replaceAll("(?i)public\\.email_templates\\s*\\(\\s*template_type,\\s*subject,\\s*html_body\\)", "public.email_templates(template_id, template_type, subject, html_body)")
                               .replaceAll("(?i)SELECT\\s+'INVOICE',", "SELECT random_uuid(), 'INVOICE',")
                               .replaceAll("(?i)SELECT\\s+'REMINDER',", "SELECT random_uuid(), 'REMINDER',")
                               .replaceAll("(?i)SELECT\\s+'WELCOME',", "SELECT random_uuid(), 'WELCOME',")
                               .replaceAll("(?i)SELECT\\s+'DUE_REMINDER',", "SELECT random_uuid(), 'DUE_REMINDER',")
                               .replaceAll("(?i)SELECT\\s+'OVERDUE_REMINDER',", "SELECT random_uuid(), 'OVERDUE_REMINDER',")
                               .replaceAll("(?i)\\s+USING\\s+[^;]+", "") // Strip out USING clauses
                               .replaceAll("(?i)VARCHAR_IGNORECASE", "VARCHAR")
                               .replaceAll("~\\*", "~");

                    String trimmed = line.trim();
                    Matcher m = alterTablePattern.matcher(line);
                    if (m.find()) {
                        if (trimmed.endsWith(";")) {
                            processedLines.add(line);
                            continue;
                        }
                        currentTable = m.group(1);
                        inAlterBlock = true;
                        continue;
                    }

                    if (inAlterBlock) {
                        if (trimmed.toUpperCase().startsWith("ADD COLUMN") || trimmed.toUpperCase().startsWith("ADD ") || trimmed.toUpperCase().startsWith("ALTER COLUMN")) {
                            boolean endsWithComma = trimmed.endsWith(",");
                            boolean endsWithSemicolon = trimmed.endsWith(";");
                            String content = trimmed;
                            if (endsWithComma) {
                                content = trimmed.substring(0, trimmed.length() - 1) + ";";
                            }
                            processedLines.add("ALTER TABLE " + currentTable + " " + content);
                            if (endsWithSemicolon) {
                                inAlterBlock = false;
                                currentTable = null;
                            }
                        } else if (trimmed.isEmpty()) {
                            processedLines.add(line);
                        } else {
                            processedLines.add("ALTER TABLE " + currentTable + " " + trimmed);
                            if (trimmed.endsWith(";")) {
                                inAlterBlock = false;
                                currentTable = null;
                            }
                        }
                    } else {
                        processedLines.add(line);
                    }
                }

                Files.write(file.toPath(), processedLines);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to preprocess migration scripts for H2 compatibility", e);
        }
    }
}
