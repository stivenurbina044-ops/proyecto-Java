package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectRegistration;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class SubjectRegistrationDao {
    public List<Subject> findAvailable(String document, int year) throws SQLException {
        String sql = """
                SELECT s.id_materia, s.codigo_materia, s.nombre_materia
                FROM materias s
                JOIN matriculas m ON m.id_grado = s.id_grado
                JOIN estudiantes e ON e.id_estudiante = m.id_estudiante
                WHERE e.numero_documento = ? AND m.`año_academico` = ?
                  AND m.estado_matricula = 'Activa' AND s.esta_activa = 1
                ORDER BY s.nombre_materia, s.id_materia
                """;
        List<Subject> subjects = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            statement.setInt(2, year);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    subjects.add(new Subject(result.getInt("id_materia"),
                            result.getString("codigo_materia"), result.getString("nombre_materia")));
                }
            }
        }
        return subjects;
    }

    public int register(String document, int year, String period, int subjectId, String section)
            throws SQLException {
        if (year < 1000 || year > 9999 || !List.of("I", "II", "III", "IV").contains(period)
                || (section != null && section.length() > 10)) {
            throw new IllegalArgumentException("Revisa el año, el período y la sección.");
        }
        String sql = """
                INSERT INTO inscripciones_materia (
                    id_estudiante, id_materia, `año_academico`, periodo_academico,
                    seccion, fecha_inscripcion, estado
                )
                SELECT e.id_estudiante, s.id_materia, m.`año_academico`, ?, ?, CURRENT_DATE, 'Activa'
                FROM estudiantes e
                JOIN matriculas m ON m.id_estudiante = e.id_estudiante
                JOIN materias s ON s.id_grado = m.id_grado
                WHERE e.numero_documento = ? AND m.`año_academico` = ?
                  AND m.estado_matricula = 'Activa'
                  AND s.id_materia = ? AND s.esta_activa = 1
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, period);
            statement.setString(2, section == null || section.isBlank() ? null : section.strip());
            statement.setString(3, document);
            statement.setInt(4, year);
            statement.setInt(5, subjectId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException(
                        "Revisa la matrícula activa, el año y la materia del grado. No se guardó la inscripción.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Subject registration ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public List<SubjectRegistration> findByStudent(String document) throws SQLException {
        String sql = """
                SELECT s.nombre_materia, i.`año_academico`, i.periodo_academico, i.seccion, i.estado
                FROM inscripciones_materia i
                JOIN estudiantes e ON e.id_estudiante = i.id_estudiante
                JOIN materias s ON s.id_materia = i.id_materia
                WHERE e.numero_documento = ?
                ORDER BY i.`año_academico` DESC, i.periodo_academico, s.nombre_materia, i.id_inscripcion
                """;
        List<SubjectRegistration> registrations = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    registrations.add(new SubjectRegistration(result.getString("nombre_materia"),
                            result.getInt("año_academico"), result.getString("periodo_academico"),
                            result.getString("seccion"), result.getString("estado")));
                }
            }
        }
        return registrations;
    }
}
