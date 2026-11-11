package com.littlehorizons.erp.util;

import com.littlehorizons.erp.dao.StudentDao;
import java.sql.SQLException;

public class DatabaseConnectionCheck {
    public static void main(String[] args) {
        try {
            int count = new StudentDao().countStudents();
            System.out.println("Estudiantes registrados: " + count);
        } catch (SQLException exception) {
            System.err.println("No se pudo consultar MySQL. " + exception.getMessage());
        }
    }
}
