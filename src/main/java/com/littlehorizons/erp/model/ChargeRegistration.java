package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record ChargeRegistration(int year, String period, String reference, LocalDate dueDate) {
    public ChargeRegistration {
        if (year < 1000 || year > 9999 || period == null || !PaymentRegistration.PERIODS.contains(period)
                || reference == null || reference.isBlank() || reference.strip().length() > 50
                || dueDate == null || dueDate.getYear() < 1000 || dueDate.getYear() > 9999) {
            throw new IllegalArgumentException("Revisa el año, período, referencia y fecha de vencimiento del cobro.");
        }
        reference = reference.strip();
    }
}
