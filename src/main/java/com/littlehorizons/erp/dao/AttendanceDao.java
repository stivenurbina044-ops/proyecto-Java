package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.WeeklyAbsence;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import com.littlehorizons.erp.model.GradingOption;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class AttendanceDao {
    public static final List<String> STATUSES = List.of("Presente", "Ausente", "Tardanza", "Excusa Médica");
    private static final String ELIGIBLE_REGISTRATIONS = """
            FROM inscripciones_materia i
            JOIN estudiantes e ON e.id_estudiante = i.id_estudiante
            JOIN materias m ON m.id_materia = i.id_materia
            JOIN matriculas enrollment ON enrollment.id_estudiante = i.id_estudiante
                 AND enrollment.`año_academico` = i.`año_academico`
                 AND enrollment.id_grado = m.id_grado
            JOIN asignaciones_profesor a ON a.id_materia = i.id_materia
                 AND a.`año_academico` = i.`año_academico` AND a.seccion <=> i.seccion
            JOIN profesores p ON p.id_profesor = a.id_profesor
            WHERE e.numero_documento = ? AND i.`año_academico` = ?
              AND i.periodo_academico = ? AND i.estado = 'Activa'
              AND e.estado = 'Activo' AND m.esta_activa = 1
              AND enrollment.estado_matricula = 'Activa'
              AND p.estado = 'Activo' AND p.fecha_contratacion <= ?
              AND i.fecha_inscripcion <= ? AND enrollment.fecha_matricula <= ?
            """;

    public List<GradingOption> findOptions(String document, LocalDate date, String period) throws SQLException {
        validateDateAndPeriod(date, period);
        String sql = """
                SELECT DISTINCT m.id_materia, m.nombre_materia, p.id_profesor,
                       CONCAT_WS(' ', p.primer_nombre, NULLIF(p.segundo_nombre, ''),
                                 p.primer_apellido, NULLIF(p.segundo_apellido, '')) AS nombre_profesor
                """ + ELIGIBLE_REGISTRATIONS + " ORDER BY m.nombre_materia, p.id_profesor";
        List<GradingOption> options = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            bindEligibility(statement, 1, document, date, period);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    options.add(new GradingOption(result.getInt("id_materia"), result.getString("nombre_materia"),
                            result.getInt("id_profesor"), result.getString("nombre_profesor")));
                }
            }
        }
        return options;
    }

    public int register(String document, LocalDate date, String period, GradingOption option,
                        String status, String notes) throws SQLException {
        validateDateAndPeriod(date, period);
        if (!STATUSES.contains(status) || option == null || notes == null || notes.length() > 1000) {
            throw new IllegalArgumentException("Estado u observaciones de asistencia inválidos.");
        }
        String sql = """
                INSERT INTO asistencias (id_estudiante, id_materia, fecha_asistencia,
                                         estado_asistencia, observaciones, registrado_por)
                SELECT DISTINCT e.id_estudiante, m.id_materia, ?, ?, ?, p.id_profesor
                """ + ELIGIBLE_REGISTRATIONS + " AND m.id_materia = ? AND p.id_profesor = ?";
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setDate(1, Date.valueOf(date));
            statement.setString(2, status);
            statement.setString(3, notes.isBlank() ? null : notes.strip());
            bindEligibility(statement, 4, document, date, period);
            statement.setInt(10, option.subjectId());
            statement.setInt(11, option.teacherId());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("La inscripción, matrícula o asignación ya no permite registrar esta asistencia.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Attendance ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    private void validateDateAndPeriod(LocalDate date, String period) {
        if (date == null || date.getYear() < 1000 || date.isAfter(LocalDate.now())
                || !List.of("I", "II", "III", "IV").contains(period)) {
            throw new IllegalArgumentException("La asistencia requiere una fecha no futura y un período I, II, III o IV.");
        }
    }

    private void bindEligibility(PreparedStatement statement, int offset, String document,
                                 LocalDate date, String period) throws SQLException {
        statement.setString(offset, document);
        statement.setInt(offset + 1, date.getYear());
        statement.setString(offset + 2, period);
        statement.setDate(offset + 3, Date.valueOf(date));
        statement.setDate(offset + 4, Date.valueOf(date));
        statement.setDate(offset + 5, Date.valueOf(date));
    }

    public List<WeeklyAbsence> findWeeklyAbsences(LocalDate referenceDate) throws SQLException {
        if (referenceDate == null || referenceDate.getYear() < 1000
                || referenceDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Selecciona una fecha válida que no sea futura para el reporte.");
        }
        LocalDate monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        String sql = """
                SELECT e.id_estudiante, e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, NULLIF(e.segundo_nombre, ''),
                                 e.primer_apellido, NULLIF(e.segundo_apellido, '')) AS nombre_estudiante,
                       COUNT(DISTINCT a.fecha_asistencia) AS dias_ausente
                FROM asistencias a
                JOIN estudiantes e ON e.id_estudiante = a.id_estudiante
                WHERE a.estado_asistencia = 'Ausente'
                  AND a.fecha_asistencia BETWEEN ? AND ?
                GROUP BY e.id_estudiante, e.numero_documento, e.primer_nombre,
                         e.segundo_nombre, e.primer_apellido, e.segundo_apellido
                HAVING COUNT(DISTINCT a.fecha_asistencia) > 3
                ORDER BY dias_ausente DESC, e.primer_apellido, e.primer_nombre, e.id_estudiante
                """;
        List<WeeklyAbsence> report = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(monday));
            statement.setDate(2, Date.valueOf(sunday));
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    report.add(new WeeklyAbsence(result.getInt("id_estudiante"),
                            result.getString("numero_documento"), result.getString("nombre_estudiante"),
                            result.getInt("dias_ausente")));
                }
            }
        }
        return report;
    }

    public List<Attendance> findByStudent(String document) throws SQLException {
        String sql = """
                SELECT attendance.id_asistencia, m.nombre_materia, attendance.fecha_asistencia,
                       attendance.estado_asistencia, attendance.observaciones,
                       CONCAT_WS(' ', p.primer_nombre, NULLIF(p.segundo_nombre, ''),
                                 p.primer_apellido, NULLIF(p.segundo_apellido, '')) AS nombre_profesor
                FROM asistencias attendance
                JOIN estudiantes e ON e.id_estudiante = attendance.id_estudiante
                JOIN materias m ON m.id_materia = attendance.id_materia
                LEFT JOIN profesores p ON p.id_profesor = attendance.registrado_por
                WHERE e.numero_documento = ?
                ORDER BY attendance.fecha_asistencia DESC, m.nombre_materia, attendance.id_asistencia
                """;
        List<Attendance> records = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    records.add(new Attendance(result.getInt("id_asistencia"), result.getString("nombre_materia"),
                            result.getDate("fecha_asistencia").toLocalDate(), result.getString("estado_asistencia"),
                            result.getString("nombre_profesor"), result.getString("observaciones")));
                }
            }
        }
        return records;
    }
}
