package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class GradeDao {
    public List<Grade> findAll() throws SQLException {
        String sql = """
                SELECT id_grado, nombre_grado, nivel_numerico
                FROM grados
                ORDER BY nivel_numerico
                """;
        List<Grade> grades = new ArrayList<>();

        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                grades.add(new Grade(result.getInt("id_grado"),
                        result.getString("nombre_grado"), result.getInt("nivel_numerico")));
            }
        }
        return grades;
    }
}
