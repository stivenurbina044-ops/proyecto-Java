package com.littlehorizons.erp.model;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record SchoolBillingDetails(String name, String taxId, String address, String phone,
        String email, String resolution, Path outputDirectory) {
    public static SchoolBillingDetails load() throws IOException {
        Properties properties = new Properties();
        try (var stream = SchoolBillingDetails.class.getResourceAsStream("/school.properties")) {
            if (stream == null) {
                throw new IOException("No se encontró school.properties en los recursos del proyecto.");
            }
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
        String override = System.getenv("INVOICE_CONFIG_PATH");
        if (override != null && !override.isBlank()) {
            try (var reader = Files.newBufferedReader(Path.of(override), StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        }
        return new SchoolBillingDetails(required(properties, "school.name"), required(properties, "school.taxId"),
                required(properties, "school.address"), required(properties, "school.phone"),
                required(properties, "school.email"), required(properties, "invoice.resolution"),
                Path.of(required(properties, "invoice.outputDirectory")).toAbsolutePath().normalize());
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank() || value.length() > 500) {
            throw new IOException("Configura " + key + " con 1 a 500 caracteres.");
        }
        return value.strip();
    }
}
