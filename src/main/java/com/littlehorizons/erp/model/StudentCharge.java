package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentCharge(int id, String studentDocument, String studentName, String conceptName,
        int year, String period, String reference, BigDecimal amount, BigDecimal paid,
        LocalDate dueDate, LocalDate referenceDate) {
    public BigDecimal balance() { return amount.subtract(paid); }
    public ChargeStatus status() { return ChargeStatus.calculate(balance(), dueDate, referenceDate); }
}
