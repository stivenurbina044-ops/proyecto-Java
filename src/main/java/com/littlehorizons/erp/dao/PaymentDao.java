package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.PaymentGuardian;
import com.littlehorizons.erp.model.PaymentRegistration;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class PaymentDao {
    public List<PaymentGuardian> findGuardians(String studentDocument) throws SQLException {
        String sql = """
                SELECT a.id_acudiente, a.numero_documento, a.es_responsable_financiero,
                       CONCAT_WS(' ', a.primer_nombre, NULLIF(a.segundo_nombre, ''),
                                 a.primer_apellido, NULLIF(a.segundo_apellido, '')) AS nombre
                FROM estudiante_acudiente link
                JOIN estudiantes e ON e.id_estudiante = link.id_estudiante
                JOIN acudientes a ON a.id_acudiente = link.id_acudiente
                WHERE e.numero_documento = ?
                ORDER BY a.es_responsable_financiero DESC, a.primer_apellido, a.id_acudiente
                """;
        List<PaymentGuardian> guardians = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, studentDocument);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    guardians.add(new PaymentGuardian(result.getInt("id_acudiente"), result.getString("numero_documento"),
                            result.getString("nombre"), result.getBoolean("es_responsable_financiero")));
                }
            }
        }
        return guardians;
    }

    public int insert(String studentDocument, int guardianId, int conceptId, PaymentRegistration payment) throws SQLException {
        String sql = """
                INSERT INTO pagos (id_estudiante, id_acudiente, id_concepto, monto_pagado,
                                   fecha_pago, metodo_pago, numero_recibo, `año_academico`, periodo_academico, notas)
                SELECT e.id_estudiante, a.id_acudiente, c.id_concepto, ?, ?, ?, ?, ?, ?, ?
                FROM estudiantes e
                JOIN estudiante_acudiente link ON link.id_estudiante = e.id_estudiante
                JOIN acudientes a ON a.id_acudiente = link.id_acudiente
                JOIN conceptos_pago c ON c.id_concepto = ? AND c.esta_activo = TRUE
                WHERE e.numero_documento = ? AND a.id_acudiente = ? AND ? <= CURRENT_DATE
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setBigDecimal(1, payment.amount());
            statement.setDate(2, Date.valueOf(payment.date()));
            statement.setString(3, payment.method());
            statement.setString(4, payment.receiptNumber());
            statement.setInt(5, payment.academicYear());
            statement.setString(6, payment.period());
            statement.setString(7, payment.notes().isEmpty() ? null : payment.notes());
            statement.setInt(8, conceptId);
            statement.setString(9, studentDocument);
            statement.setInt(10, guardianId);
            statement.setDate(11, Date.valueOf(payment.date()));
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se guardó el pago. Revisa concepto activo, vínculo del acudiente y fecha no futura.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Payment ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public List<Payment> findByStudent(String document) throws SQLException {
        return findHistory(document, false);
    }

    public List<Payment> findByGuardian(String document) throws SQLException {
        return findHistory(document, true);
    }

    private List<Payment> findHistory(String document, boolean byGuardian) throws SQLException {
        String sql = """
                SELECT p.id_pago, e.numero_documento,
                       CONCAT_WS(' ', e.primer_nombre, NULLIF(e.segundo_nombre, ''),
                                 e.primer_apellido, NULLIF(e.segundo_apellido, '')) AS estudiante,
                       CONCAT_WS(' ', a.primer_nombre, NULLIF(a.segundo_nombre, ''),
                                 a.primer_apellido, NULLIF(a.segundo_apellido, '')) AS acudiente,
                       c.nombre_concepto, p.monto_pagado, p.fecha_pago, p.metodo_pago,
                       p.numero_recibo, p.`año_academico`, p.periodo_academico, p.notas
                FROM pagos p
                JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                LEFT JOIN acudientes a ON a.id_acudiente = p.id_acudiente
                JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
                """ + (byGuardian ? " WHERE a.numero_documento = ?" : " WHERE e.numero_documento = ?")
                + " ORDER BY p.fecha_pago DESC, p.id_pago DESC";
        List<Payment> payments = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    payments.add(new Payment(result.getInt("id_pago"), result.getString("numero_documento"),
                            result.getString("estudiante"), result.getString("acudiente"), result.getString("nombre_concepto"),
                            result.getBigDecimal("monto_pagado"), result.getDate("fecha_pago").toLocalDate(),
                            result.getString("metodo_pago"), result.getString("numero_recibo"),
                            result.getObject("año_academico", Integer.class), result.getString("periodo_academico"),
                            result.getString("notas")));
                }
            }
        }
        return payments;
    }
}
