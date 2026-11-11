package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record Attendance(int id, String subjectName, LocalDate date, String status,
        String teacherName, String notes) {
}
