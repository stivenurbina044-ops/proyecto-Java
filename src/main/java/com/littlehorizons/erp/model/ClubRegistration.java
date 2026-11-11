package com.littlehorizons.erp.model;

public record ClubRegistration(String name, String description, int supervisorId,
        int capacity, String meetingSchedule) {
    public ClubRegistration {
        if (name == null || name.isBlank() || name.strip().length() > 100
                || supervisorId <= 0 || capacity <= 0 || description == null
                || description.length() > 1000 || meetingSchedule == null || meetingSchedule.length() > 100) {
            throw new IllegalArgumentException("El club requiere nombre, profesor encargado y cupo mayor que cero.");
        }
        name = name.strip();
        description = description.strip();
        meetingSchedule = meetingSchedule.strip();
    }
}
