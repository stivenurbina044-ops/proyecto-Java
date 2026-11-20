package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.util.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GradeCertificateDao {
    public record Mark(String subjectCode, String subjectName, String period, BigDecimal value) { }

    public record StudentGrades(String name, String documentType, String document, String grade, List<Mark> marks) { }

    public Optional<StudentGrades> find(int studentId, int year) throws SQLException {
        String sql = """
                SELECT CONCAT_WS(' ', TRIM(e.primer_nombre), NULLIF(TRIM(e.segundo_nombre), ''),
                           TRIM(e.primer_apellido), NULLIF(TRIM(e.segundo_apellido), '')) AS nombre,
                       e.tipo_documento, e.numero_documento, g.nombre_grado,
                       m.codigo_materia, m.nombre_materia, r.periodo_academico, r.valor_nota
                FROM estudiantes e
                LEFT JOIN matriculas t ON t.id_estudiante = e.id_estudiante AND t.`año_academico` = ?
                LEFT JOIN grados g ON g.id_grado = t.id_grado
                LEFT JOIN registros_academicos r ON r.id_estudiante = e.id_estudiante AND r.`año_academico` = ?
                LEFT JOIN materias m ON m.id_materia = r.id_materia
                WHERE e.id_estudiante = ?
                ORDER BY m.nombre_materia, m.codigo_materia, r.periodo_academico
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            statement.setInt(2, year);
            statement.setInt(3, studentId);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) {
                    return Optional.empty();
                }
                String name = rows.getString("nombre");
                String documentType = rows.getString("tipo_documento");
                String document = rows.getString("numero_documento");
                String grade = rows.getString("nombre_grado");
                List<Mark> marks = new ArrayList<>();
                do {
                    String code = rows.getString("codigo_materia");
                    if (code != null) {
                        marks.add(new Mark(code, rows.getString("nombre_materia"),
                                rows.getString("periodo_academico"), rows.getBigDecimal("valor_nota")));
                    }
                } while (rows.next());
                return Optional.of(new StudentGrades(name, documentType, document, grade, List.copyOf(marks)));
            }
        }
    }
}
