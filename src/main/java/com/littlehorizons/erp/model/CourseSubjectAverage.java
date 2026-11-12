package com.littlehorizons.erp.model;

import java.math.BigDecimal;

public record CourseSubjectAverage(int subjectId, String subjectCode, String subjectName,
        int year, String period, int gradedStudents, BigDecimal average) {
}
