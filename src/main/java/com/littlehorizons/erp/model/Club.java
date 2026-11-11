package com.littlehorizons.erp.model;

public record Club(int id, String name, String description, String supervisorName,
        Integer capacity, int activeMembers, String meetingSchedule, boolean active) {
}
