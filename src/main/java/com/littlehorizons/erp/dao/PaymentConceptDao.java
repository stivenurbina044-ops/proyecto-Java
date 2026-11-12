package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentConceptRegistration;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class PaymentConceptDao {
    public List<PaymentConcept> findAll() throws SQLException {
        String sql = """
                SELECT id_concepto, codigo_concepto, nombre_concepto, descripcion,
                       monto_base, es_recurrente, esta_activo
                FROM conceptos_pago ORDER BY nombre_concepto, id_concepto
                """;
        List<PaymentConcept> concepts = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                concepts.add(new PaymentConcept(result.getInt("id_concepto"), result.getString("codigo_concepto"),
                        result.getString("nombre_concepto"), result.getString("descripcion"),
                        result.getBigDecimal("monto_base"), result.getBoolean("es_recurrente"), result.getBoolean("esta_activo")));
            }
        }
        return concepts;
    }

    public int insert(PaymentConceptRegistration concept) throws SQLException {
        String sql = """
                INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion,
                                           monto_base, es_recurrente, esta_activo)
                VALUES (?, ?, ?, ?, ?, TRUE)
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, concept.code());
            statement.setString(2, concept.name());
            statement.setString(3, concept.description().isEmpty() ? null : concept.description());
            statement.setBigDecimal(4, concept.baseAmount());
            statement.setBoolean(5, concept.recurring());
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Payment concept ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }
}
