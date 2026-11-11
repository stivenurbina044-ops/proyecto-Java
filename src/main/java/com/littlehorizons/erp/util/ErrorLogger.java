package com.littlehorizons.erp.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;

public final class ErrorLogger {
    private static final Path LOG_PATH = Path.of("error_log.txt");

    private ErrorLogger() {
    }

    public static synchronized void log(String operation, Exception exception) {
        StringWriter details = new StringWriter();
        try (PrintWriter writer = new PrintWriter(details)) {
            writer.println(OffsetDateTime.now() + " | " + operation);
            exception.printStackTrace(writer);
            writer.println();
        }
        try {
            Files.writeString(LOG_PATH, details.toString(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | SecurityException loggingException) {
            System.err.println("No se pudo escribir el registro de errores.");
        }
    }
}
