package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ChargeRegistration;
import com.littlehorizons.erp.model.StudentCharge;
import com.littlehorizons.erp.model.StudentRate;
import com.littlehorizons.erp.util.DatabaseConnection;
import com.littlehorizons.erp.util.MoneyValidation;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class StudentBillingDao {
    public void setRate(String document, int conceptId, int year, BigDecimal amount, String reason) throws SQLException {
        MoneyValidation.validate(amount, true);
        if (year < 1000 || year > 9999 || reason == null || reason.isBlank() || reason.strip().length() > 1000) {
            throw new IllegalArgumentException("La tarifa requiere año válido y motivo de 1 a 1000 caracteres.");
        }
        String sql = """
                INSERT INTO tarifas_estudiante (id_estudiante, id_concepto, `año_academico`, monto_personalizado, motivo)
                SELECT e.id_estudiante, c.id_concepto, ?, ?, ?
                FROM estudiantes e JOIN conceptos_pago c ON c.id_concepto = ? AND c.esta_activo = TRUE
                WHERE e.numero_documento = ?
                ON DUPLICATE KEY UPDATE monto_personalizado = ?, motivo = ?
                """;
        try (var connection = DatabaseConnection.open(); var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, year);
            statement.setBigDecimal(2, amount);
            statement.setString(3, reason.strip());
            statement.setInt(4, conceptId);
            statement.setString(5, document);
            statement.setBigDecimal(6, amount);
            statement.setString(7, reason.strip());
            int changed = statement.executeUpdate();
            if (changed == 0) {
                // A no-op update can return zero; verify the exact active concept and rate.
                try (var verification = connection.prepareStatement("""
                        SELECT t.id_estudiante FROM tarifas_estudiante t
                        JOIN estudiantes e ON e.id_estudiante = t.id_estudiante
                        JOIN conceptos_pago c ON c.id_concepto = t.id_concepto AND c.esta_activo = TRUE
                        WHERE e.numero_documento = ? AND t.id_concepto = ? AND t.`año_academico` = ?
                          AND t.monto_personalizado = ? AND t.motivo = ?
                        """)) {
                    verification.setString(1, document);
                    verification.setInt(2, conceptId);
                    verification.setInt(3, year);
                    verification.setBigDecimal(4, amount);
                    verification.setString(5, reason.strip());
                    try (var result = verification.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El estudiante no existe o el concepto no está activo.");
                        }
                    }
                }
            }
        }
    }

    public List<StudentRate> findRates(String document) throws SQLException {
        String sql = """
                SELECT c.nombre_concepto, t.`año_academico`, t.monto_personalizado, t.motivo
                FROM tarifas_estudiante t
                JOIN estudiantes e ON e.id_estudiante = t.id_estudiante
                JOIN conceptos_pago c ON c.id_concepto = t.id_concepto
                WHERE e.numero_documento = ? ORDER BY t.`año_academico` DESC, c.nombre_concepto
                """;
        List<StudentRate> rates = new ArrayList<>();
        try (var connection = DatabaseConnection.open(); var statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    rates.add(new StudentRate(result.getString("nombre_concepto"), result.getInt("año_academico"),
                            result.getBigDecimal("monto_personalizado"), result.getString("motivo")));
                }
            }
        }
        return rates;
    }

    public int createCharge(String document, int conceptId, ChargeRegistration charge) throws SQLException {
        String sql = """
                INSERT INTO cobros_estudiante (id_estudiante, id_concepto, `año_academico`,
                        periodo_academico, referencia, monto_cobrado, fecha_vencimiento)
                SELECT e.id_estudiante, c.id_concepto, ?, ?, ?, COALESCE(t.monto_personalizado, c.monto_base), ?
                FROM estudiantes e JOIN conceptos_pago c ON c.id_concepto = ? AND c.esta_activo = TRUE
                LEFT JOIN tarifas_estudiante t ON t.id_estudiante = e.id_estudiante
                     AND t.id_concepto = c.id_concepto AND t.`año_academico` = ?
                WHERE e.numero_documento = ?
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, charge.year());
            statement.setString(2, charge.period());
            statement.setString(3, charge.reference());
            statement.setDate(4, Date.valueOf(charge.dueDate()));
            statement.setInt(5, conceptId);
            statement.setInt(6, charge.year());
            statement.setString(7, document);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("El estudiante no existe o el concepto no está activo.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Student charge ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public List<StudentCharge> findCharges(String document, boolean overdueOnly) throws SQLException {
        String sql = """
                SELECT b.id_cobro, e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, NULLIF(e.segundo_nombre, ''),
                           e.primer_apellido, NULLIF(e.segundo_apellido, '')) AS estudiante,
                       c.nombre_concepto, b.`año_academico`, b.periodo_academico, b.referencia,
                       b.monto_cobrado, b.fecha_vencimiento, CURRENT_DATE AS fecha_actual,
                       COALESCE(a.pagado, 0) AS pagado
                FROM cobros_estudiante b JOIN estudiantes e ON e.id_estudiante = b.id_estudiante
                JOIN conceptos_pago c ON c.id_concepto = b.id_concepto
                LEFT JOIN (SELECT ap.id_cobro, SUM(p.monto_pagado) AS pagado
                    FROM aplicaciones_pago ap JOIN pagos p ON p.id_pago = ap.id_pago
                    GROUP BY ap.id_cobro) a ON a.id_cobro = b.id_cobro
                """;
        sql += overdueOnly ? " WHERE b.fecha_vencimiento < DATE_SUB(CURRENT_DATE, INTERVAL 1 MONTH)"
                + " AND b.monto_cobrado > COALESCE(a.pagado, 0)" : " WHERE e.numero_documento = ?";
        sql += " ORDER BY b.fecha_vencimiento, b.id_cobro";
        List<StudentCharge> charges = new ArrayList<>();
        try (var connection = DatabaseConnection.open(); var statement = connection.prepareStatement(sql)) {
            if (!overdueOnly) {
                statement.setString(1, document);
            }
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    charges.add(new StudentCharge(result.getInt("id_cobro"), result.getString("numero_documento"),
                            result.getString("estudiante"), result.getString("nombre_concepto"), result.getInt("año_academico"),
                            result.getString("periodo_academico"), result.getString("referencia"),
                            result.getBigDecimal("monto_cobrado"), result.getBigDecimal("pagado"),
                            result.getDate("fecha_vencimiento").toLocalDate(), result.getDate("fecha_actual").toLocalDate()));
                }
            }
        }
        return charges;
    }

    public void applyPayment(String document, int paymentId, int chargeId) throws SQLException {
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int studentId;
                int conceptId;
                int year;
                String period;
                BigDecimal amount;
                try (var statement = connection.prepareStatement("""
                        SELECT p.id_estudiante, p.id_concepto, p.`año_academico`, p.periodo_academico, p.monto_pagado
                        FROM pagos p JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                        WHERE p.id_pago = ? AND e.numero_documento = ? FOR UPDATE
                        """)) {
                    statement.setInt(1, paymentId);
                    statement.setString(2, document);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El pago no pertenece al estudiante consultado.");
                        }
                        studentId = result.getInt("id_estudiante");
                        conceptId = result.getInt("id_concepto");
                        year = result.getInt("año_academico");
                        period = result.getString("periodo_academico");
                        amount = result.getBigDecimal("monto_pagado");
                        MoneyValidation.validate(amount, false);
                    }
                }
                BigDecimal charged;
                try (var statement = connection.prepareStatement("""
                        SELECT monto_cobrado FROM cobros_estudiante
                        WHERE id_cobro = ? AND id_estudiante = ? AND id_concepto = ?
                          AND `año_academico` = ? AND periodo_academico = ? FOR UPDATE
                        """)) {
                    statement.setInt(1, chargeId);
                    statement.setInt(2, studentId);
                    statement.setInt(3, conceptId);
                    statement.setInt(4, year);
                    statement.setString(5, period);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El cobro debe coincidir con estudiante, concepto, año y período del pago.");
                        }
                        charged = result.getBigDecimal("monto_cobrado");
                    }
                }
                try (var statement = connection.prepareStatement("SELECT id_pago FROM aplicaciones_pago WHERE id_pago = ? FOR UPDATE")) {
                    statement.setInt(1, paymentId);
                    try (var result = statement.executeQuery()) {
                        if (result.next()) {
                            throw new IllegalArgumentException("El pago ya está aplicado a un cobro.");
                        }
                    }
                }
                BigDecimal paid = BigDecimal.ZERO;
                try (var statement = connection.prepareStatement("""
                        SELECT p.monto_pagado FROM aplicaciones_pago ap JOIN pagos p ON p.id_pago = ap.id_pago
                        WHERE ap.id_cobro = ? FOR UPDATE
                        """)) {
                    statement.setInt(1, chargeId);
                    try (var result = statement.executeQuery()) {
                        while (result.next()) {
                            paid = paid.add(result.getBigDecimal("monto_pagado"));
                        }
                    }
                }
                if (paid.add(amount).compareTo(charged) > 0) {
                    throw new IllegalArgumentException("El pago supera el saldo pendiente. No se divide ni aplica automáticamente.");
                }
                try (var statement = connection.prepareStatement("""
                        INSERT INTO aplicaciones_pago (id_pago, id_cobro, fecha_aplicacion) VALUES (?, ?, CURRENT_DATE)
                        """)) {
                    statement.setInt(1, paymentId);
                    statement.setInt(2, chargeId);
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private void rollback(Connection connection, Exception exception) {
        try {
            connection.rollback();
        } catch (SQLException rollbackException) {
            exception.addSuppressed(rollbackException);
        }
    }
}
