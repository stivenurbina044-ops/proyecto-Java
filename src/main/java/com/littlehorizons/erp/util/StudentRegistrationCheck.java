package com.littlehorizons.erp.util;

import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.dao.StudentGuardianDao;
import com.littlehorizons.erp.service.StudentRegistrationService;
import java.sql.SQLException;
import java.time.LocalDate;

public final class StudentRegistrationCheck {
    public static void main(String[] args) {
        var service = new StudentRegistrationService(
                new StudentDao(), new GuardianDao(), new StudentGuardianDao());

        try {
            int studentId = service.registerWithExistingGuardian(
                    "TI", "DEMO-TI-002", "Mateo", "Torres", LocalDate.of(2016, 5, 14),
                    "M", LocalDate.of(2026, 2, 2), "DEMO-CC-001");
            System.out.println("Mateo Torres fue registrado y vinculado a su acudiente. ID: " + studentId);
        } catch (IllegalArgumentException exception) {
            System.err.println(exception.getMessage());
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1062) {
                System.err.println("El documento DEMO-TI-002 ya está registrado. Consulta el listado.");
            } else {
                System.err.println("No se pudo completar el registro: " + exception.getMessage());
            }
        }
    }
}
