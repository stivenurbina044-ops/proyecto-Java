package com.littlehorizons.erp.util;

import com.littlehorizons.erp.dao.GuardianDao;
import java.sql.SQLException;

public class GuardianDaoCheck {
    public static void main(String[] args) {
        GuardianDao guardianDao = new GuardianDao();

        try (var connection = DatabaseConnection.open()) {
            var existing = guardianDao.findGuardianIdByDocument(connection, "DEMO-CC-001");
            var missing = guardianDao.findGuardianIdByDocument(connection, "DEMO-NO-EXISTE");

            System.out.println("Documento de Mariana: " + existing);
            System.out.println("Documento inexistente: " + missing);
        } catch (SQLException exception) {
            System.err.println("Error de MySQL: " + exception.getMessage());
        }
    }
}