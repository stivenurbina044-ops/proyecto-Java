package com.littlehorizons.erp.model;

import java.math.BigDecimal;

public record StudentSubjectAverage(int subjectId, String subjectCode, String subjectName,
        int year, int recordedPeriods, BigDecimal periodAverage, BigDecimal finalGrade) {
}
