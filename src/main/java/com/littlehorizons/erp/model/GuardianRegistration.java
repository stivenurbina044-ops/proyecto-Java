package com.littlehorizons.erp.model;

public record GuardianRegistration(
        String documentType, String documentNumber, String firstName, String lastName,
        String relationship, String primaryPhone, boolean financiallyResponsible) {
}
