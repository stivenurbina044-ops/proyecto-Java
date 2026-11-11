package com.littlehorizons.erp.util;

import com.littlehorizons.erp.dao.StudentDao;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

public final class StudentInsertCheck {
    public static void main(String[] args) {
        String documentNumber = "DEMO-" + UUID.randomUUID().toString().substring(0, 8);

        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            int studentId;
            try {
                studentId = new StudentDao().insertStudent(
                        connection, "TI", documentNumber, "Mateo", "Torres",
                        LocalDate.of(2016, 5, 14), "M", LocalDate.of(2026, 2, 2));
            } finally {
                connection.rollback();
            }

            System.out.println("ID generado: " + studentId);
            System.out.println("Transacción revertida. El estudiante de prueba no quedó guardado.");
        } catch (SQLException exception) {
            System.err.println("No se pudo completar la prueba: " + exception.getMessage());
        }
    }
}
