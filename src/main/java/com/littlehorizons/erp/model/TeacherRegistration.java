package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record TeacherRegistration(String documentType, String documentNumber,
        String firstName, String middleName, String lastName, String secondLastName,
        String email, String phone, String specialization, LocalDate hireDate) {
}
