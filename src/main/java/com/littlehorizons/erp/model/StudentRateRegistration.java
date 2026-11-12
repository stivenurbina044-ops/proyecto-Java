package com.littlehorizons.erp.model;

import com.littlehorizons.erp.util.MoneyValidation;
import java.math.BigDecimal;

public record StudentRateRegistration(int year, BigDecimal amount, String reason) {
    public StudentRateRegistration {
        MoneyValidation.validate(amount, true);
        if (year < 1000 || year > 9999 || reason == null || reason.isBlank() || reason.strip().length() > 1000) {
            throw new IllegalArgumentException("La tarifa requiere un año válido y un motivo de 1 a 1000 caracteres.");
        }
        reason = reason.strip();
    }
}
