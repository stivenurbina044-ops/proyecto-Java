package com.littlehorizons.erp.model;

import com.littlehorizons.erp.util.MoneyValidation;
import java.math.BigDecimal;

public record PaymentConceptRegistration(String code, String name, String description,
        BigDecimal baseAmount, boolean recurring) {
    public PaymentConceptRegistration {
        if (code == null || code.isBlank() || code.strip().length() > 20
                || name == null || name.isBlank() || name.strip().length() > 100
                || description == null || description.length() > 1000) {
            throw new IllegalArgumentException("Revisa el código, nombre y descripción del concepto.");
        }
        MoneyValidation.validate(baseAmount, true);
        code = code.strip();
        name = name.strip();
        description = description.strip();
    }
}
