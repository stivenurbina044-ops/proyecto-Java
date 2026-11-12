package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Payment(int id, String studentDocument, String studentName, String guardianName,
        String conceptName, BigDecimal amount, LocalDate date, String method, String receiptNumber,
        Integer academicYear, String period, String notes) {
}
