package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class TeacherAssignmentDao {
    public List<TeacherAssignment> findAll() throws SQLException {
        String sql = """
                SELECT a.id_asignacion, a.`año_academico`, a.seccion,
                       CONCAT_WS(' ', p.primer_nombre, NULLIF(p.segundo_nombre, ''),
                                 p.primer_apellido, NULLIF(p.segundo_apellido, '')) AS nombre_profesor,
                       m.codigo_materia, m.nombre_materia
                FROM asignaciones_profesor a
                JOIN profesores p ON p.id_profesor = a.id_profesor
                JOIN materias m ON m.id_materia = a.id_materia
                ORDER BY a.`año_academico` DESC, p.primer_apellido, p.primer_nombre,
                         m.codigo_materia, a.seccion, a.id_asignacion
                """;
        List<TeacherAssignment> assignments = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                assignments.add(new TeacherAssignment(result.getInt("id_asignacion"),
                        result.getString("nombre_profesor"), result.getString("codigo_materia"),
                        result.getString("nombre_materia"), result.getInt("año_academico"),
                        result.getString("seccion")));
            }
        }
        return assignments;
    }

    public List<Subject> findActiveSubjects() throws SQLException {
        List<Subject> subjects = new ArrayList<>();
        String sql = "SELECT id_materia, codigo_materia, nombre_materia FROM materias "
                + "WHERE esta_activa = 1 ORDER BY nombre_materia, codigo_materia";
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                subjects.add(new Subject(result.getInt("id_materia"), result.getString("codigo_materia"),
                        result.getString("nombre_materia")));
            }
        }
        return subjects;
    }

    public int assign(int teacherId, int subjectId, int year, String section) throws SQLException {
        if (year < 1000 || year > 9999 || section == null || section.strip().length() > 10) {
            throw new IllegalArgumentException("Año o sección inválidos.");
        }
        String normalizedSection = section.strip().isEmpty() ? null : section.strip();
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                // Serialize assignments for this teacher, including sections stored as NULL.
                try (var statement = connection.prepareStatement("""
                        SELECT id_profesor FROM profesores
                        WHERE id_profesor = ? AND estado = 'Activo'
                          AND fecha_contratacion <= CURRENT_DATE
                          AND YEAR(fecha_contratacion) <= ? FOR UPDATE
                        """)) {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, year);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El profesor no está activo o su contratación no permite ese año.");
                        }
                    }
                }
                try (var statement = connection.prepareStatement(
                        "SELECT id_materia FROM materias WHERE id_materia = ? AND esta_activa = 1 FOR UPDATE")) {
                    statement.setInt(1, subjectId);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("La materia ya no está activa.");
                        }
                    }
                }
                try (var statement = connection.prepareStatement("""
                        SELECT id_asignacion FROM asignaciones_profesor
                        WHERE id_profesor = ? AND id_materia = ? AND `año_academico` = ?
                          AND seccion <=> ? FOR UPDATE
                        """)) {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, year);
                    statement.setString(4, normalizedSection);
                    try (var result = statement.executeQuery()) {
                        if (result.next()) {
                            throw new IllegalArgumentException("Ya existe esa asignación para el profesor, materia, año y sección.");
                        }
                    }
                }
                int id;
                try (var statement = connection.prepareStatement("""
                        INSERT INTO asignaciones_profesor (id_profesor, id_materia, `año_academico`, seccion)
                        VALUES (?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, subjectId);
                    statement.setInt(3, year);
                    statement.setString(4, normalizedSection);
                    statement.executeUpdate();
                    try (var keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Assignment ID was not generated.");
                        }
                        id = keys.getInt(1);
                    }
                }
                connection.commit();
                return id;
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
}
