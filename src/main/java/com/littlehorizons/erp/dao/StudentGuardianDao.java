package com.littlehorizons.erp.dao;

import java.sql.Connection;
import java.sql.SQLException;

public final class StudentGuardianDao {
    public void linkPrimaryGuardian(Connection connection, int studentId, int guardianId)
            throws SQLException {
        String sql = """
                INSERT INTO estudiante_acudiente (
                    id_estudiante, id_acudiente, es_principal, es_contacto_emergencia
                ) VALUES (?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            statement.setInt(2, guardianId);
            statement.setBoolean(3, true);
            statement.setBoolean(4, true);
            statement.executeUpdate();
        }
    }
}
