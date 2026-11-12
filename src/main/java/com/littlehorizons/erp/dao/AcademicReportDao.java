package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ReportGrade;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AcademicReportDao {
    public List<ReportGrade> findByStudent(String document, int year) throws SQLException {
        validateYear(year);
        String sql = """
                SELECT r.id_estudiante, m.id_materia, m.codigo_materia, m.nombre_materia,
                       r.`año_academico`, r.periodo_academico, r.valor_nota
                FROM registros_academicos r
                JOIN estudiantes e ON e.id_estudiante = r.id_estudiante
                JOIN materias m ON m.id_materia = r.id_materia
                WHERE e.numero_documento = ? AND r.`año_academico` = ?
                ORDER BY m.codigo_materia, r.periodo_academico
                """;
        try (var connection = DatabaseConnection.open(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            statement.setInt(2, year);
            try (var result = statement.executeQuery()) {
                return readGrades(result);
            }
        }
    }

    public List<ReportGrade> findByGrade(int gradeId, int year, String period) throws SQLException {
        validateYear(year);
        if (gradeId <= 0 || period == null || !List.of("I", "II", "III", "IV", "Final").contains(period)) {
            throw new IllegalArgumentException("Selecciona un grado y período válidos.");
        }
        String sql = """
                SELECT r.id_estudiante, m.id_materia, m.codigo_materia, m.nombre_materia,
                       r.`año_academico`, r.periodo_academico, r.valor_nota
                FROM registros_academicos r
                JOIN materias m ON m.id_materia = r.id_materia
                JOIN matriculas enrollment ON enrollment.id_estudiante = r.id_estudiante
                    AND enrollment.`año_academico` = r.`año_academico` AND enrollment.id_grado = m.id_grado
                WHERE enrollment.id_grado = ? AND r.`año_academico` = ? AND r.periodo_academico = ?
                ORDER BY m.codigo_materia, r.id_estudiante
                """;
        try (var connection = DatabaseConnection.open(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, gradeId);
            statement.setInt(2, year);
            statement.setString(3, period);
            try (var result = statement.executeQuery()) {
                return readGrades(result);
            }
        }
    }

    private List<ReportGrade> readGrades(ResultSet result) throws SQLException {
        List<ReportGrade> grades = new ArrayList<>();
        while (result.next()) {
            grades.add(new ReportGrade(result.getInt("id_estudiante"), result.getInt("id_materia"),
                    result.getString("codigo_materia"), result.getString("nombre_materia"), result.getInt("año_academico"),
                    result.getString("periodo_academico"), result.getBigDecimal("valor_nota")));
        }
        return grades;
    }

    private void validateYear(int year) {
        if (year < 1000 || year > 9999) {
            throw new IllegalArgumentException("El año debe tener cuatro cifras.");
        }
    }
}
