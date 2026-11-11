package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record SchoolResource(int id, String code, String name, String category,
        String description, LocalDate acquisitionDate, String location, String status) {
}
