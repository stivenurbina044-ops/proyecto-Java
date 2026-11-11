package com.littlehorizons.erp.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private DatabaseConnection() {
    }

    public static Connection open() throws SQLException {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || url.isBlank() || user == null || user.isBlank() || password == null) {
            throw new SQLException("Configura DB_URL, DB_USER y DB_PASSWORD en IntelliJ.");
        }

        return DriverManager.getConnection(url, user, password);
    }
}
