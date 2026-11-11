package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.ClassScheduleRegistration;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.List;

public final class ClassScheduleDao {
    public int register(int assignmentId, ClassScheduleRegistration schedule) throws SQLException {
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int teacherId;
                int year;
                // Lock the teacher so concurrent registrations cannot bypass conflict checks.
                try (var statement = connection.prepareStatement("""
                        SELECT a.id_profesor, a.`año_academico`
                        FROM asignaciones_profesor a
                        JOIN profesores p ON p.id_profesor = a.id_profesor
                        JOIN materias m ON m.id_materia = a.id_materia
                        WHERE a.id_asignacion = ? AND p.estado = 'Activo'
                          AND p.fecha_contratacion <= CURRENT_DATE
                          AND YEAR(p.fecha_contratacion) <= a.`año_academico`
                          AND m.esta_activa = 1
                        FOR UPDATE
                        """)) {
                    statement.setInt(1, assignmentId);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("La asignación no existe o su profesor o materia no están habilitados.");
                        }
                        teacherId = result.getInt("id_profesor");
                        year = result.getInt("año_academico");
                    }
                }
                try (var statement = connection.prepareStatement("""
                        SELECT h.id_horario FROM horarios_clase h
                        JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion
                        WHERE a.id_profesor = ? AND a.`año_academico` = ?
                          AND h.dia_semana = ? AND h.hora_inicio < ? AND h.hora_fin > ?
                        FOR UPDATE
                        """)) {
                    statement.setInt(1, teacherId);
                    statement.setInt(2, year);
                    statement.setInt(3, schedule.dayOfWeek());
                    statement.setTime(4, Time.valueOf(schedule.endTime()));
                    statement.setTime(5, Time.valueOf(schedule.startTime()));
                    try (var result = statement.executeQuery()) {
                        if (result.next()) {
                            throw new IllegalArgumentException("El profesor ya tiene una clase que se cruza con ese horario en el mismo año.");
                        }
                    }
                }
                int id;
                try (var statement = connection.prepareStatement("""
                        INSERT INTO horarios_clase (id_asignacion, dia_semana, hora_inicio, hora_fin)
                        VALUES (?, ?, ?, ?)
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, assignmentId);
                    statement.setInt(2, schedule.dayOfWeek());
                    statement.setTime(3, Time.valueOf(schedule.startTime()));
                    statement.setTime(4, Time.valueOf(schedule.endTime()));
                    statement.executeUpdate();
                    try (var keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Schedule ID was not generated.");
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

    public List<ClassSchedule> findAll() throws SQLException {
        String sql = """
                SELECT h.id_horario, h.id_asignacion, h.dia_semana, h.hora_inicio, h.hora_fin,
                       a.`año_academico`, a.seccion, m.codigo_materia,
                       CONCAT_WS(' ', p.primer_nombre, NULLIF(p.segundo_nombre, ''),
                                 p.primer_apellido, NULLIF(p.segundo_apellido, '')) AS nombre_profesor
                FROM horarios_clase h
                JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion
                JOIN profesores p ON p.id_profesor = a.id_profesor
                JOIN materias m ON m.id_materia = a.id_materia
                ORDER BY a.`año_academico` DESC, p.id_profesor, h.dia_semana, h.hora_inicio, h.id_horario
                """;
        List<ClassSchedule> schedules = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                schedules.add(new ClassSchedule(result.getInt("id_horario"), result.getInt("id_asignacion"),
                        result.getString("nombre_profesor"), result.getString("codigo_materia"),
                        result.getInt("año_academico"), result.getString("seccion"),
                        result.getInt("dia_semana"), result.getTime("hora_inicio").toLocalTime(),
                        result.getTime("hora_fin").toLocalTime()));
            }
        }
        return schedules;
    }
}
