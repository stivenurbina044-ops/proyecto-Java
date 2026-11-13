package com.littlehorizons.erp.model;

import java.time.LocalDate;

public record StudentRegistration(
        String documentType, String documentNumber, String firstName, String lastName,
        LocalDate birthDate, String gender, LocalDate admissionDate, String guardianDocumentNumber,
        String middleName, String secondLastName, String address) {
}
