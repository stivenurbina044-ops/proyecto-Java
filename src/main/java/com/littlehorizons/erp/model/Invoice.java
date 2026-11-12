package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record Invoice(int id, int paymentId, String number, LocalDate issueDate, String cufe,
        String fileName, String filePath) {
}
