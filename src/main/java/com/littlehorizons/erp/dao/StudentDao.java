package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Student;
import com.littlehorizons.erp.model.StudentRegistration;
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

    public void update(StudentRegistration student) throws SQLException {
        String sql = """
                UPDATE estudiantes e SET primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?,
                    segundo_apellido = ?, direccion = ?, fecha_nacimiento = ?, genero = ?
                WHERE e.numero_documento = ? AND e.tipo_documento = ? AND EXISTS (
                    SELECT 1 FROM estudiante_acudiente link JOIN acudientes a ON a.id_acudiente = link.id_acudiente
                    WHERE link.id_estudiante = e.id_estudiante AND a.numero_documento = ?)
                  AND e.fecha_ingreso >= ?
                """;
        try (var connection = DatabaseConnection.open(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.firstName());
            statement.setString(2, student.middleName());
            statement.setString(3, student.lastName());
            statement.setString(4, student.secondLastName());
            statement.setString(5, student.address());
            statement.setDate(6, Date.valueOf(student.birthDate()));
            statement.setString(7, student.gender());
            statement.setString(8, student.documentNumber());
            statement.setString(9, student.documentType());
            statement.setString(10, student.guardianDocumentNumber());
            statement.setDate(11, Date.valueOf(student.birthDate()));
            if (statement.executeUpdate() != 1) {
                throw new IllegalArgumentException("Revisa documento, tipo, acudiente vinculado y nacimiento anterior al ingreso; los datos pueden no haber cambiado.");
            }
        }
    }

    public int insertStudent(Connection connection, StudentRegistration student) throws SQLException {
        int id = insertStudent(connection, student.documentType(), student.documentNumber(),
                student.firstName(), student.lastName(), student.birthDate(), student.gender(), student.admissionDate());
        try (var statement = connection.prepareStatement(
                "UPDATE estudiantes SET segundo_nombre = ?, segundo_apellido = ?, direccion = ? WHERE id_estudiante = ?")) {
            statement.setString(1, student.middleName());
            statement.setString(2, student.secondLastName());
            statement.setString(3, student.address());
            statement.setInt(4, id);
            statement.executeUpdate();
        }
        return id;
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
