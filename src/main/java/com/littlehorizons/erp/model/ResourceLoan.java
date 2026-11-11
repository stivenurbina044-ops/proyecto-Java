package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record ResourceLoan(int id, BorrowerType borrowerType, int resourceId, String resourceCode,
        String resourceName, String borrowerDocument, String borrowerName, LocalDate loanDate,
        LocalDate expectedReturnDate, LocalDate actualReturnDate, String status, String notes) {
}
