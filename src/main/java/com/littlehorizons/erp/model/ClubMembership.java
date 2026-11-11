package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record ClubMembership(int id, String clubName, LocalDate entryDate,
        String status, LocalDate withdrawalDate) {
}
