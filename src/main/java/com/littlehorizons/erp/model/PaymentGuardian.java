package com.littlehorizons.erp.model;

public record PaymentGuardian(int id, String documentNumber, String fullName, boolean financiallyResponsible) {
}
