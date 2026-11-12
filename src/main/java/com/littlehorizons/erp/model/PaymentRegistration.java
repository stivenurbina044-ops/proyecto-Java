package com.littlehorizons.erp.model;

import com.littlehorizons.erp.util.MoneyValidation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PaymentRegistration(BigDecimal amount, LocalDate date, String method,
        String receiptNumber, int academicYear, String period, String notes) {
    public static final List<String> METHODS = List.of("Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito", "Cheque");
    public static final List<String> PERIODS = List.of("I", "II", "III", "IV", "Anual");

    public PaymentRegistration {
        MoneyValidation.validate(amount, false);
        if (date == null || date.getYear() < 1000 || date.isAfter(LocalDate.now())
                || method == null || !METHODS.contains(method)
                || receiptNumber == null || receiptNumber.isBlank() || receiptNumber.strip().length() > 50
                || academicYear < 1000 || academicYear > 9999 || period == null || !PERIODS.contains(period)
                || notes == null || notes.length() > 1000) {
            throw new IllegalArgumentException("Revisa fecha, método, recibo, año y período del pago.");
        }
        receiptNumber = receiptNumber.strip();
        notes = notes.strip();
    }
}
