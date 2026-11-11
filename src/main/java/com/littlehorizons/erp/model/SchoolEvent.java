package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record SchoolEvent(int id, String name, String description, String type, LocalDate date,
        String location, boolean competition, String organizerName, boolean active) {
}
