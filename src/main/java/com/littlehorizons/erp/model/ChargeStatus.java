package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public enum ChargeStatus {
    PENDING("Pendiente"), OVERDUE("Vencido"), PAID("Saldado");

    private final String label;

    ChargeStatus(String label) {
        this.label = label;
    }

    public String label() { return label; }

    public static ChargeStatus calculate(BigDecimal balance, LocalDate dueDate, LocalDate referenceDate) {
        if (balance.signum() <= 0) {
            return PAID;
        }
        return dueDate.isBefore(referenceDate) ? OVERDUE : PENDING;
    }
}
