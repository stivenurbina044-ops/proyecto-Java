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
                FROM estudiantes WHERE numero_documento = ? AND estado = 'Activo'
                  AND fecha_ingreso <= CURRENT_DATE AND YEAR(fecha_ingreso) <= ?
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, gradeId);
            statement.setInt(2, academicYear);
            statement.setDate(3, Date.valueOf(LocalDate.now()));
            statement.setString(4, studentDocument);
            statement.setInt(5, academicYear);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("El estudiante debe estar activo y haber ingresado al colegio para el año seleccionado.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Enrollment ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public void closeEnrollment(String document, int year, boolean graduated) throws SQLException {
        if (year < 1000 || year > 9999) {
            throw new IllegalArgumentException("El año académico debe tener cuatro cifras.");
        }
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int studentId;
                try (var statement = connection.prepareStatement(
                        "SELECT id_estudiante FROM estudiantes WHERE numero_documento = ? FOR UPDATE")) {
                    statement.setString(1, document);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El estudiante no existe.");
                        }
                        studentId = result.getInt(1);
                    }
                }
                // Closing the last school grade is required for graduation.
                String sql = """
                        UPDATE matriculas m JOIN grados g ON g.id_grado = m.id_grado
                        SET m.estado_matricula = ?, m.fecha_retiro = ?
                        WHERE m.id_estudiante = ? AND m.`año_academico` = ?
                          AND m.estado_matricula = 'Activa' AND m.fecha_matricula <= CURRENT_DATE
                          AND m.`año_academico` <= YEAR(CURRENT_DATE)
                        """ + (graduated ? " AND g.nivel_numerico = 11" : "");
                try (var statement = connection.prepareStatement(sql)) {
                    statement.setString(1, graduated ? "Completada" : "Retirada");
                    statement.setDate(2, graduated ? null : Date.valueOf(LocalDate.now()));
                    statement.setInt(3, studentId);
                    statement.setInt(4, year);
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("Revisa matrícula activa, año y fecha. La graduación requiere grado 11.");
                    }
                }
                try (var statement = connection.prepareStatement("""
                        UPDATE inscripciones_materia SET estado = ?
                        WHERE id_estudiante = ? AND `año_academico` = ? AND estado = 'Activa'
                        """)) {
                    statement.setString(1, graduated ? "Completada" : "Retirada");
                    statement.setInt(2, studentId);
                    statement.setInt(3, year);
                    statement.executeUpdate();
                }
                // Historical closures must not deactivate a student enrolled in another year.
                try (var statement = connection.prepareStatement("""
                        UPDATE estudiantes SET estado = ? WHERE id_estudiante = ?
                        AND NOT EXISTS (SELECT 1 FROM matriculas WHERE id_estudiante = ? AND estado_matricula = 'Activa')
                        """)) {
                    statement.setString(1, graduated ? "Graduado" : "Retirado");
                    statement.setInt(2, studentId);
                    statement.setInt(3, studentId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
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
