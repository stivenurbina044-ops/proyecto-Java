package com.littlehorizons.erp.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AcademicRecord(String subjectName, int year, String period,
                             BigDecimal grade, LocalDate registrationDate) {
}
