package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record Enrollment(int id, String studentName, String documentNumber, String gradeName,
                         int academicYear, String status, LocalDate enrollmentDate) {
}
