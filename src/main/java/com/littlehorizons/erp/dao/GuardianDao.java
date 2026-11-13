package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.GuardianRegistration;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.OptionalInt;

public class GuardianDao {
    public OptionalInt findGuardianIdByDocument(Connection connection, String documentNumber)
            throws SQLException {
        String sql = "SELECT id_acudiente FROM acudientes WHERE numero_documento = ?";

        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, documentNumber);

            try (var result = statement.executeQuery()) {
                return result.next()
                        ? OptionalInt.of(result.getInt("id_acudiente"))
                        : OptionalInt.empty();
            }
        }
    }

    public int insertGuardian(GuardianRegistration guardian) throws SQLException {
        String sql = """
                INSERT INTO acudientes (
                    tipo_documento, numero_documento, primer_nombre, primer_apellido,
                    parentesco, telefono_principal, es_responsable_financiero,
                    segundo_nombre, segundo_apellido, direccion
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, guardian.documentType());
            statement.setString(2, guardian.documentNumber());
            statement.setString(3, guardian.firstName());
            statement.setString(4, guardian.lastName());
            statement.setString(5, guardian.relationship());
            statement.setString(6, guardian.primaryPhone());
            statement.setBoolean(7, guardian.financiallyResponsible());
            statement.setString(8, guardian.middleName());
            statement.setString(9, guardian.secondLastName());
            statement.setString(10, guardian.address());
            statement.executeUpdate();

            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Guardian ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }
}
