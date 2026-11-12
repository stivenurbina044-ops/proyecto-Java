package com.littlehorizons.erp.util;

import java.math.BigDecimal;

public final class MoneyValidation {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    private MoneyValidation() {
    }

    public static void validate(BigDecimal amount, boolean allowZero) {
        if (amount == null || amount.scale() > 2 || amount.signum() < 0
                || (!allowZero && amount.signum() == 0) || amount.compareTo(MAX_AMOUNT) > 0) {
            throw new IllegalArgumentException(allowZero
                    ? "El valor debe estar entre 0 y 99999999,99 con máximo dos decimales."
                    : "El monto debe ser mayor que cero, máximo 99999999,99 y dos decimales.");
        }
    }
}
