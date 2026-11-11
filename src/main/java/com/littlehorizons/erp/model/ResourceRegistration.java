package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record ResourceRegistration(String code, String name, int categoryId, String description,
        LocalDate acquisitionDate, String location) {
    public ResourceRegistration {
        if (code == null || code.isBlank() || code.strip().length() > 50
                || name == null || name.isBlank() || name.strip().length() > 150 || categoryId <= 0
                || description == null || description.length() > 1000
                || location == null || location.isBlank() || location.strip().length() > 100
                || (acquisitionDate != null && (acquisitionDate.getYear() < 1000
                    || acquisitionDate.isAfter(LocalDate.now())))) {
            throw new IllegalArgumentException("Datos del recurso inválidos. Revisa código, nombre, categoría, ubicación y fecha.");
        }
        code = code.strip();
        name = name.strip();
        description = description.strip();
        location = location.strip();
    }
}
