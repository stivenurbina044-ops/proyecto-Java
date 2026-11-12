package com.littlehorizons.erp.model;

import java.math.BigDecimal;

public record StudentRate(String conceptName, int year, BigDecimal amount, String reason) {
}
