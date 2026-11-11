package com.littlehorizons.erp.model;

import java.time.LocalTime;

public record ClassSchedule(int id, int assignmentId, String teacherName, String subjectCode,
        int year, String section, int dayOfWeek, LocalTime startTime, LocalTime endTime) {
}
