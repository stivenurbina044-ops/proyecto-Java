package com.littlehorizons.erp.model;

import java.math.BigDecimal;

public record ReportGrade(int studentId, int subjectId, String subjectCode, String subjectName,
        int year, String period, BigDecimal value) {
}
