package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Date;
import com.littlehorizons.erp.model.TeacherRegistration;
import java.util.ArrayList;
import java.util.List;

public final class TeacherDao {
    public List<Teacher> findAll() throws SQLException {
        String sql = """
                SELECT id_profesor, numero_documento, estado,
                       CONCAT_WS(' ', primer_nombre, NULLIF(segundo_nombre, ''),
                                 primer_apellido, NULLIF(segundo_apellido, '')) AS nombre_completo
                FROM profesores
                ORDER BY primer_apellido, primer_nombre, id_profesor
                """;
        List<Teacher> teachers = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                teachers.add(new Teacher(result.getInt("id_profesor"),
                        result.getString("numero_documento"), result.getString("nombre_completo"),
                        result.getString("estado")));
            }
        }
        return teachers;
    }
    public int insert(TeacherRegistration teacher) throws SQLException {
        String sql = """
                INSERT INTO profesores (tipo_documento, numero_documento, primer_nombre,
                    segundo_nombre, primer_apellido, segundo_apellido, correo_electronico,
                    telefono, especializacion, fecha_contratacion, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                    CASE WHEN ? > CURRENT_DATE THEN 'Inactivo' ELSE 'Activo' END)
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, teacher.documentType());
            statement.setString(2, teacher.documentNumber());
            statement.setString(3, teacher.firstName());
            statement.setString(4, teacher.middleName());
            statement.setString(5, teacher.lastName());
            statement.setString(6, teacher.secondLastName());
            statement.setString(7, teacher.email());
            statement.setString(8, teacher.phone());
            statement.setString(9, teacher.specialization());
            statement.setDate(10, Date.valueOf(teacher.hireDate()));
            statement.setDate(11, Date.valueOf(teacher.hireDate()));
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Teacher ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }
}
