package com.littlehorizons.erp.model;

import java.math.BigDecimal;

public record PaymentConcept(int id, String code, String name, String description,
        BigDecimal baseAmount, boolean recurring, boolean active) {
}
