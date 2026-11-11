package com.littlehorizons.erp.model;

public record TeacherAssignment(int id, String teacherName, String subjectCode,
        String subjectName, int year, String section) {
}
