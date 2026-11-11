package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.ResourceLoan;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ResourceLoanDao {
    public int lend(int resourceId, BorrowerType type, String document,
                    LocalDate expectedReturn, String notes) throws SQLException {
        if (type == null || document == null || document.isBlank() || document.length() > 20
                || expectedReturn == null || expectedReturn.getYear() > 9999
                || expectedReturn.isBefore(LocalDate.now()) || notes == null || notes.length() > 1000) {
            throw new IllegalArgumentException("El préstamo requiere documento, fecha de devolución de hoy o posterior y observaciones válidas.");
        }
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                if (!"Disponible".equals(lockResource(connection, resourceId))) {
                    throw new IllegalArgumentException("El recurso no está disponible para préstamo.");
                }
                if (countOutstanding(connection, resourceId) != 0) {
                    throw new IllegalArgumentException("El recurso ya tiene un préstamo sin cerrar. Revisa su historial.");
                }
                int borrowerId;
                // SQL identifiers come only from the fixed enum, never from user input.
                String borrowerSql = "SELECT " + type.borrowerColumn() + " FROM " + type.borrowerTable()
                        + " WHERE numero_documento = ? AND estado = 'Activo' AND "
                        + type.startDateColumn() + " <= CURRENT_DATE FOR UPDATE";
                try (var statement = connection.prepareStatement(borrowerSql)) {
                    statement.setString(1, document.strip());
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El solicitante no existe, está inactivo o aún no ha iniciado su vinculación al colegio.");
                        }
                        borrowerId = result.getInt(1);
                    }
                }
                int id;
                String insertSql = "INSERT INTO " + type.loanTable() + " (id_recurso, "
                        + type.borrowerColumn() + ", fecha_prestamo, fecha_devolucion_esperada, estado_prestamo, observaciones) "
                        + "SELECT ?, ?, CURRENT_DATE, ?, 'Activo', ? WHERE ? >= CURRENT_DATE";
                try (var statement = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, resourceId);
                    statement.setInt(2, borrowerId);
                    statement.setDate(3, Date.valueOf(expectedReturn));
                    statement.setString(4, notes.isBlank() ? null : notes.strip());
                    statement.setDate(5, Date.valueOf(expectedReturn));
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("La fecha de devolución no puede ser anterior a la fecha actual de MySQL.");
                    }
                    try (var keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Resource loan ID was not generated.");
                        }
                        id = keys.getInt(1);
                    }
                }
                updateResourceStatus(connection, resourceId, "Prestado");
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    public void returnResource(BorrowerType type, int loanId) throws SQLException {
        if (type == null || loanId <= 0) {
            throw new IllegalArgumentException("Selecciona un préstamo válido.");
        }
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int resourceId;
                try (var statement = connection.prepareStatement(
                        "SELECT id_recurso FROM " + type.loanTable() + " WHERE id_prestamo = ?")) {
                    statement.setInt(1, loanId);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El préstamo ya no existe.");
                        }
                        resourceId = result.getInt("id_recurso");
                    }
                }
                if (!"Prestado".equals(lockResource(connection, resourceId))) {
                    throw new IllegalArgumentException("El recurso no figura como prestado. Revisa sus datos antes de devolverlo.");
                }
                if (countOutstanding(connection, resourceId) != 1) {
                    throw new IllegalArgumentException("El recurso tiene un historial de préstamos inconsistente. Revisa sus datos.");
                }
                String sql = "UPDATE " + type.loanTable()
                        + " SET estado_prestamo = 'Devuelto', fecha_devolucion_real = CURRENT_DATE"
                        + " WHERE id_prestamo = ? AND id_recurso = ?"
                        + " AND estado_prestamo IN ('Activo', 'Vencido') AND fecha_devolucion_real IS NULL"
                        + " AND fecha_prestamo <= CURRENT_DATE";
                try (var statement = connection.prepareStatement(sql)) {
                    statement.setInt(1, loanId);
                    statement.setInt(2, resourceId);
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("El préstamo ya fue devuelto o su fecha de préstamo es futura.");
                    }
                }
                updateResourceStatus(connection, resourceId, "Disponible");
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        }
    }

    private String lockResource(Connection connection, int resourceId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT estado_recurso FROM recursos WHERE id_recurso = ? FOR UPDATE")) {
            statement.setInt(1, resourceId);
            try (var result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new IllegalArgumentException("El recurso ya no existe.");
                }
                return result.getString("estado_recurso");
            }
        }
    }

    private int countOutstanding(Connection connection, int resourceId) throws SQLException {
        int count = 0;
        for (BorrowerType type : BorrowerType.values()) {
            String sql = "SELECT id_prestamo FROM " + type.loanTable()
                    + " WHERE id_recurso = ? AND (estado_prestamo IN ('Activo', 'Vencido')"
                    + " OR fecha_devolucion_real IS NULL) FOR UPDATE";
            try (var statement = connection.prepareStatement(sql)) {
                statement.setInt(1, resourceId);
                try (var result = statement.executeQuery()) {
                    while (result.next()) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void updateResourceStatus(Connection connection, int resourceId, String status) throws SQLException {
        try (var statement = connection.prepareStatement(
                "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?")) {
            statement.setString(1, status);
            statement.setInt(2, resourceId);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Resource status was not updated.");
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

    public List<ResourceLoan> findAll(boolean overdueOnly) throws SQLException {
        List<ResourceLoan> loans = new ArrayList<>();
        try (var connection = DatabaseConnection.open()) {
            for (BorrowerType type : BorrowerType.values()) {
                String sql = """
                        SELECT l.id_prestamo, l.id_recurso, r.codigo_recurso, r.nombre_recurso,
                               b.numero_documento, l.fecha_prestamo, l.fecha_devolucion_esperada,
                               l.fecha_devolucion_real, l.observaciones,
                               CONCAT_WS(' ', b.primer_nombre, NULLIF(b.segundo_nombre, ''),
                                         b.primer_apellido, NULLIF(b.segundo_apellido, '')) AS solicitante,
                               CASE WHEN l.fecha_devolucion_real IS NULL
                                          AND l.estado_prestamo IN ('Activo', 'Vencido')
                                          AND l.fecha_devolucion_esperada < CURRENT_DATE THEN 'Vencido'
                                    ELSE l.estado_prestamo END AS estado_actual
                        FROM %s l
                        JOIN recursos r ON r.id_recurso = l.id_recurso
                        JOIN %s b ON b.%s = l.%s
                        """.formatted(type.loanTable(), type.borrowerTable(), type.borrowerColumn(), type.borrowerColumn());
                if (overdueOnly) {
                    sql += " WHERE l.fecha_devolucion_real IS NULL AND l.estado_prestamo IN ('Activo', 'Vencido')"
                            + " AND l.fecha_devolucion_esperada < CURRENT_DATE";
                }
                try (var statement = connection.prepareStatement(sql);
                     var result = statement.executeQuery()) {
                    while (result.next()) {
                        var actualReturn = result.getDate("fecha_devolucion_real");
                        loans.add(new ResourceLoan(result.getInt("id_prestamo"), type, result.getInt("id_recurso"),
                                result.getString("codigo_recurso"), result.getString("nombre_recurso"),
                                result.getString("numero_documento"), result.getString("solicitante"),
                                result.getDate("fecha_prestamo").toLocalDate(),
                                result.getDate("fecha_devolucion_esperada").toLocalDate(),
                                actualReturn == null ? null : actualReturn.toLocalDate(),
                                result.getString("estado_actual"), result.getString("observaciones")));
                    }
                }
            }
        }
        return loans.stream().sorted(Comparator.comparing(ResourceLoan::expectedReturnDate)
                .thenComparing(ResourceLoan::borrowerType).thenComparingInt(ResourceLoan::id)).toList();
    }
}
