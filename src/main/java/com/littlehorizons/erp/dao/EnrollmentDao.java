package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class EnrollmentDao {
    public int enroll(String studentDocument, int gradeId, int academicYear) throws SQLException {
        if (academicYear < 1000 || academicYear > 9999) {
            throw new IllegalArgumentException("El año académico debe tener cuatro cifras.");
        }
        String sql = """
                INSERT INTO matriculas (
                    id_estudiante, id_grado, `año_academico`, estado_matricula, fecha_matricula
                )
                SELECT id_estudiante, ?, ?, 'Activa', ?
                FROM estudiantes WHERE numero_documento = ?
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, gradeId);
            statement.setInt(2, academicYear);
            statement.setDate(3, Date.valueOf(LocalDate.now()));
            statement.setString(4, studentDocument);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No existe un estudiante con ese documento.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Enrollment ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public List<Enrollment> findAll() throws SQLException {
        String sql = """
                SELECT m.id_matricula, e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, NULLIF(e.segundo_nombre, ''),
                                 e.primer_apellido, NULLIF(e.segundo_apellido, '')) AS nombre_completo,
                       g.nombre_grado, m.`año_academico`, m.estado_matricula, m.fecha_matricula
                FROM matriculas m
                JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                JOIN grados g ON g.id_grado = m.id_grado
                ORDER BY m.`año_academico` DESC, g.nivel_numerico, nombre_completo, m.id_matricula
                """;
        List<Enrollment> enrollments = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                enrollments.add(new Enrollment(result.getInt("id_matricula"),
                        result.getString("nombre_completo"), result.getString("numero_documento"),
                        result.getString("nombre_grado"), result.getInt("año_academico"),
                        result.getString("estado_matricula"), result.getDate("fecha_matricula").toLocalDate()));
            }
        }
        return enrollments;
    }
}
