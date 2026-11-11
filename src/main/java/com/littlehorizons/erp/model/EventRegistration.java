package com.littlehorizons.erp.model;

import java.time.LocalDate;
import java.util.List;

public record EventRegistration(String name, String description, String type, LocalDate date,
        String location, boolean competition, int organizerId) {
    public static final List<String> TYPES = List.of("Académico", "Cultural", "Deportivo", "Social", "Científico");

    public EventRegistration {
        if (name == null || name.isBlank() || name.strip().length() > 150
                || description == null || description.length() > 1000
                || type == null || !TYPES.contains(type) || date == null
                || date.getYear() < 1000 || date.getYear() > 9999
                || location == null || location.length() > 150 || organizerId <= 0) {
            throw new IllegalArgumentException("Datos del evento inválidos. Revisa nombre, tipo, fecha y organizador.");
        }
        name = name.strip();
        description = description.strip();
        location = location.strip();
    }
}
