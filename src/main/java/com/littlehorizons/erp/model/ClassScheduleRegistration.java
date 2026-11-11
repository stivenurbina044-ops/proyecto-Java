package com.littlehorizons.erp.model;

import java.time.LocalTime;

public record ClassScheduleRegistration(int dayOfWeek, LocalTime startTime, LocalTime endTime) {
    public ClassScheduleRegistration {
        if (dayOfWeek < 1 || dayOfWeek > 7 || startTime == null || endTime == null
                || !startTime.isBefore(endTime) || startTime.getSecond() != 0
                || endTime.getSecond() != 0 || startTime.getNano() != 0 || endTime.getNano() != 0) {
            throw new IllegalArgumentException("El horario requiere un día válido y horas HH:mm con inicio anterior al fin.");
        }
    }
}
