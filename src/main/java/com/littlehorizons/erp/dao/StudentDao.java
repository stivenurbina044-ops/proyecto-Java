package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class StudentDao {
    public int countStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM estudiantes";

        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            if (!result.next()) {
                throw new SQLException("Student count query returned no result.");
            }
            return result.getInt(1);
        }
    }

    public List<Student> findAll() throws SQLException {
        String sql = """
                SELECT id_estudiante, tipo_documento, numero_documento, estado,
                       CONCAT_WS(' ', primer_nombre, NULLIF(segundo_nombre, ''),
                                 primer_apellido, NULLIF(segundo_apellido, '')) AS nombre_completo
                FROM estudiantes
                ORDER BY primer_apellido, segundo_apellido, primer_nombre, id_estudiante
                """;
        List<Student> students = new ArrayList<>();

        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                students.add(new Student(
                        result.getInt("id_estudiante"),
                        result.getString("tipo_documento"),
                        result.getString("numero_documento"),
                        result.getString("nombre_completo"),
                        result.getString("estado")));
            }
        }
        return students;
    }

    public int insertStudent(Connection connection, String documentType, String documentNumber,
                             String firstName, String lastName, LocalDate birthDate,
                             String gender, LocalDate admissionDate) throws SQLException {
        String sql = """
                INSERT INTO estudiantes (
                    tipo_documento, numero_documento, primer_nombre, primer_apellido,
                    fecha_nacimiento, genero, fecha_ingreso
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, documentType);
            statement.setString(2, documentNumber);
            statement.setString(3, firstName);
            statement.setString(4, lastName);
            statement.setDate(5, Date.valueOf(birthDate));
            statement.setString(6, gender);
            statement.setDate(7, Date.valueOf(admissionDate));
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Student ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }
}
