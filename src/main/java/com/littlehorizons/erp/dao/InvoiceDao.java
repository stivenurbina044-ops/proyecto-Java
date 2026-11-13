package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoicePayment;
import com.littlehorizons.erp.util.MoneyValidation;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

public class InvoiceDao {
    public Optional<byte[]> findDocument(Connection connection, int invoiceId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT contenido_pdf FROM documentos_factura WHERE id_factura = ?")) {
            statement.setInt(1, invoiceId);
            try (var result = statement.executeQuery()) {
                return result.next() ? Optional.of(result.getBytes(1)) : Optional.empty();
            }
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 1146) {
                throw new SQLException("Ejecuta docs/migrations/003_invoice_documents.sql para habilitar el archivo de facturas.",
                        exception.getSQLState(), exception.getErrorCode(), exception);
            }
            throw exception;
        }
    }

    public void saveDocument(Connection connection, int invoiceId, byte[] pdf) throws SQLException {
        // An issued document is immutable; retries must never replace its original bytes.
        try (var statement = connection.prepareStatement(
                "INSERT INTO documentos_factura (id_factura, contenido_pdf) VALUES (?, ?) ON DUPLICATE KEY UPDATE id_factura = documentos_factura.id_factura")) {
            statement.setInt(1, invoiceId);
            statement.setBytes(2, pdf);
            statement.executeUpdate();
        }
    }

    public InvoicePayment lockPayment(Connection connection, int paymentId) throws SQLException {
        String sql = """
                SELECT p.id_pago, p.monto_pagado, p.fecha_pago, p.metodo_pago, p.numero_recibo,
                       p.`año_academico`, p.periodo_academico, c.nombre_concepto,
                       e.numero_documento AS documento_estudiante,
                       CONCAT_WS(' ', e.primer_nombre, NULLIF(e.segundo_nombre, ''),
                                 e.primer_apellido, NULLIF(e.segundo_apellido, '')) AS estudiante,
                       a.tipo_documento, a.numero_documento AS documento_acudiente, a.direccion,
                       CONCAT_WS(' ', a.primer_nombre, NULLIF(a.segundo_nombre, ''),
                                 a.primer_apellido, NULLIF(a.segundo_apellido, '')) AS acudiente,
                       CURRENT_DATE AS fecha_actual
                FROM pagos p
                JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
                LEFT JOIN acudientes a ON a.id_acudiente = p.id_acudiente
                WHERE p.id_pago = ? FOR UPDATE
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, paymentId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("El pago no existe.");
                }
                if (result.getString("documento_acudiente") == null) {
                    throw new IllegalArgumentException("El pago no tiene un acudiente pagador asociado para facturar.");
                }
                MoneyValidation.validate(result.getBigDecimal("monto_pagado"), false);
                return new InvoicePayment(paymentId, result.getString("estudiante"), result.getString("documento_estudiante"),
                        result.getString("acudiente"), result.getString("tipo_documento"), result.getString("documento_acudiente"),
                        result.getString("direccion"), result.getString("nombre_concepto"), result.getBigDecimal("monto_pagado"),
                        result.getDate("fecha_pago").toLocalDate(), result.getString("metodo_pago"), result.getString("numero_recibo"),
                        result.getObject("año_academico", Integer.class), result.getString("periodo_academico"),
                        result.getDate("fecha_actual").toLocalDate());
            }
        }
    }

    public Optional<Invoice> findByPayment(Connection connection, int paymentId) throws SQLException {
        String sql = """
                SELECT id_factura, id_pago, numero_factura, fecha_emision, cufe, nombre_archivo_pdf, ruta_archivo
                FROM facturas WHERE id_pago = ? ORDER BY id_factura FOR UPDATE
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, paymentId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                Invoice invoice = new Invoice(result.getInt("id_factura"), paymentId,
                        result.getString("numero_factura"), result.getDate("fecha_emision").toLocalDate(),
                        result.getString("cufe"), result.getString("nombre_archivo_pdf"), result.getString("ruta_archivo"));
                if (result.next()) {
                    throw new IllegalArgumentException("Hay varias facturas para el mismo pago. Revisa esos registros antes de continuar.");
                }
                return Optional.of(invoice);
            }
        }
    }

    public int reserve(Connection connection, InvoicePayment payment) throws SQLException {
        try (var statement = connection.prepareStatement("""
                INSERT INTO facturas (numero_factura, id_pago, fecha_emision) VALUES (?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "PENDIENTE-" + UUID.randomUUID());
            statement.setInt(2, payment.paymentId());
            statement.setDate(3, java.sql.Date.valueOf(payment.currentDate()));
            statement.executeUpdate();
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Invoice ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public void complete(Connection connection, Invoice invoice) throws SQLException {
        try (var statement = connection.prepareStatement("""
                UPDATE facturas SET numero_factura = ?, cufe = ?, nombre_archivo_pdf = ?, ruta_archivo = ?
                WHERE id_factura = ? AND id_pago = ?
                """)) {
            statement.setString(1, invoice.number());
            statement.setString(2, invoice.cufe());
            statement.setString(3, invoice.fileName());
            statement.setString(4, invoice.filePath());
            statement.setInt(5, invoice.id());
            statement.setInt(6, invoice.paymentId());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Invoice metadata was not saved.");
            }
        }
    }
}
