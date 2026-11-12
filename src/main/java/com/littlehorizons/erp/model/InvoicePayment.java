package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoicePayment(int paymentId, String studentName, String studentDocument,
        String guardianName, String guardianDocumentType, String guardianDocument, String guardianAddress,
        String conceptName, BigDecimal amount, LocalDate paymentDate, String paymentMethod,
        String receiptNumber, Integer academicYear, String period, LocalDate currentDate) {
}
