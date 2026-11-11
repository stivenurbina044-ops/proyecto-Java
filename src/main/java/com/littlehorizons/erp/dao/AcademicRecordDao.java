package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.model.GradingOption;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class AcademicRecordDao {
    private static final String ELIGIBLE_REGISTRATIONS = """
            FROM inscripciones_materia i
            JOIN estudiantes e ON e.id_estudiante = i.id_estudiante
            JOIN materias s ON s.id_materia = i.id_materia
            JOIN asignaciones_profesor a ON a.id_materia = i.id_materia
                 AND a.`año_academico` = i.`año_academico`
                 AND a.seccion <=> i.seccion
            JOIN profesores p ON p.id_profesor = a.id_profesor
            WHERE e.numero_documento = ? AND i.`año_academico` = ?
              AND (? = 'Final' OR i.periodo_academico = ?)
              AND i.estado IN ('Activa', 'Completada') AND p.estado = 'Activo'
              AND p.fecha_contratacion <= CURRENT_DATE
            """;

    public List<GradingOption> findGradingOptions(String document, int year, String period)
            throws SQLException {
        String sql = """
                SELECT DISTINCT s.id_materia, s.nombre_materia, p.id_profesor,
                       CONCAT_WS(' ', p.primer_nombre, NULLIF(p.segundo_nombre, ''),
                                 p.primer_apellido, NULLIF(p.segundo_apellido, '')) AS nombre_profesor
                """ + ELIGIBLE_REGISTRATIONS + " ORDER BY s.nombre_materia, p.id_profesor";
        List<GradingOption> options = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            bindEligibility(statement, 1, document, year, period);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    options.add(new GradingOption(result.getInt("id_materia"),
                            result.getString("nombre_materia"), result.getInt("id_profesor"),
                            result.getString("nombre_profesor")));
                }
            }
        }
        return options;
    }

    public int insertGrade(String document, int year, String period, GradingOption option,
                           BigDecimal grade) throws SQLException {
        if (grade == null || grade.scale() > 1 || grade.signum() < 0
                || grade.compareTo(new BigDecimal("5.0")) > 0) {
            throw new IllegalArgumentException("La nota debe estar entre 0,0 y 5,0 con máximo un decimal.");
        }
        if (!List.of("I", "II", "III", "IV", "Final").contains(period)) {
            throw new IllegalArgumentException("Período inválido.");
        }
        String sql = """
                INSERT INTO registros_academicos (
                    id_estudiante, id_materia, id_profesor, `año_academico`,
                    periodo_academico, valor_nota, fecha_registro
                )
                SELECT DISTINCT e.id_estudiante, s.id_materia, p.id_profesor,
                                i.`año_academico`, ?, ?, CURRENT_DATE
                """ + ELIGIBLE_REGISTRATIONS + " AND s.id_materia = ? AND p.id_profesor = ?";
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, period);
            statement.setBigDecimal(2, grade);
            bindEligibility(statement, 3, document, year, period);
            statement.setInt(7, option.subjectId());
            statement.setInt(8, option.teacherId());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("La inscripción o asignación del profesor ya no permite registrar la nota.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Academic record ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    private void bindEligibility(PreparedStatement statement, int offset, String document,
                                 int year, String period) throws SQLException {
        statement.setString(offset, document);
        statement.setInt(offset + 1, year);
        statement.setString(offset + 2, period);
        statement.setString(offset + 3, period);
    }

    public List<AcademicRecord> findByStudent(String document) throws SQLException {
        String sql = """
                SELECT s.nombre_materia, r.`año_academico`, r.periodo_academico,
                       r.valor_nota, r.fecha_registro
                FROM registros_academicos r
                JOIN estudiantes e ON e.id_estudiante = r.id_estudiante
                JOIN materias s ON s.id_materia = r.id_materia
                WHERE e.numero_documento = ?
                ORDER BY r.`año_academico`, r.periodo_academico, s.nombre_materia, r.id_registro
                """;
        List<AcademicRecord> records = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    records.add(new AcademicRecord(result.getString("nombre_materia"),
                            result.getInt("año_academico"), result.getString("periodo_academico"),
                            result.getBigDecimal("valor_nota"), result.getDate("fecha_registro").toLocalDate()));
                }
            }
        }
        return records;
    }
}
