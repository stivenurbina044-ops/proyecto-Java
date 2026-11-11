package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.ClubRegistration;
import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ClubDao {
    public List<Club> findAll() throws SQLException {
        String sql = """
                SELECT c.id_club, c.nombre_club, c.descripcion, c.capacidad_maxima,
                       c.horario_reunion, c.esta_activo,
                       CONCAT_WS(' ', p.primer_nombre, NULLIF(p.segundo_nombre, ''),
                                 p.primer_apellido, NULLIF(p.segundo_apellido, '')) AS supervisor,
                       (SELECT COUNT(*) FROM membresias_club mc
                        WHERE mc.id_club = c.id_club AND mc.estado_membresia = 'Activa') AS miembros
                FROM clubes c
                LEFT JOIN profesores p ON p.id_profesor = c.id_profesor_supervisor
                ORDER BY c.nombre_club, c.id_club
                """;
        List<Club> clubs = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                clubs.add(new Club(result.getInt("id_club"), result.getString("nombre_club"),
                        result.getString("descripcion"), result.getString("supervisor"),
                        result.getObject("capacidad_maxima", Integer.class), result.getInt("miembros"),
                        result.getString("horario_reunion"), result.getBoolean("esta_activo")));
            }
        }
        return clubs;
    }

    public List<Teacher> findSupervisors() throws SQLException {
        String sql = """
                SELECT id_profesor, numero_documento, estado,
                       CONCAT_WS(' ', primer_nombre, NULLIF(segundo_nombre, ''),
                                 primer_apellido, NULLIF(segundo_apellido, '')) AS nombre
                FROM profesores WHERE estado = 'Activo' AND fecha_contratacion <= CURRENT_DATE
                ORDER BY primer_apellido, primer_nombre, id_profesor
                """;
        List<Teacher> teachers = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                teachers.add(new Teacher(result.getInt("id_profesor"), result.getString("numero_documento"),
                        result.getString("nombre"), result.getString("estado")));
            }
        }
        return teachers;
    }

    public int insert(ClubRegistration club) throws SQLException {
        String sql = """
                INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor,
                                    capacidad_maxima, horario_reunion, esta_activo)
                SELECT ?, ?, id_profesor, ?, ?, TRUE FROM profesores
                WHERE id_profesor = ? AND estado = 'Activo' AND fecha_contratacion <= CURRENT_DATE
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, club.name());
            statement.setString(2, club.description().isEmpty() ? null : club.description());
            statement.setInt(3, club.capacity());
            statement.setString(4, club.meetingSchedule().isEmpty() ? null : club.meetingSchedule());
            statement.setInt(5, club.supervisorId());
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("El profesor encargado ya no está habilitado.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Club ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public int enroll(String document, int clubId) throws SQLException {
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int capacity;
                // All enrollments for a club take this lock before checking available places.
                try (var statement = connection.prepareStatement("""
                        SELECT capacidad_maxima FROM clubes
                        WHERE id_club = ? AND esta_activo = TRUE FOR UPDATE
                        """)) {
                    statement.setInt(1, clubId);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El club no existe o está inactivo.");
                        }
                        capacity = result.getInt("capacidad_maxima");
                        if (result.wasNull() || capacity <= 0) {
                            throw new IllegalArgumentException("El club no tiene un cupo máximo válido configurado.");
                        }
                    }
                }
                int studentId;
                try (var statement = connection.prepareStatement("""
                        SELECT id_estudiante FROM estudiantes
                        WHERE numero_documento = ? AND estado = 'Activo'
                          AND fecha_ingreso <= CURRENT_DATE FOR UPDATE
                        """)) {
                    statement.setString(1, document);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El estudiante no existe, está inactivo o aún no ha ingresado al colegio.");
                        }
                        studentId = result.getInt("id_estudiante");
                    }
                }
                int activeMembers = 0;
                try (var statement = connection.prepareStatement("""
                        SELECT id_estudiante FROM membresias_club
                        WHERE id_club = ? AND estado_membresia = 'Activa' FOR UPDATE
                        """)) {
                    statement.setInt(1, clubId);
                    try (var result = statement.executeQuery()) {
                        while (result.next()) {
                            if (result.getInt("id_estudiante") == studentId) {
                                throw new IllegalArgumentException("El estudiante ya tiene una membresía activa en este club.");
                            }
                            activeMembers++;
                        }
                    }
                }
                if (activeMembers >= capacity) {
                    throw new IllegalArgumentException("El club ya alcanzó su cupo máximo.");
                }
                int id;
                try (var statement = connection.prepareStatement("""
                        INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso, estado_membresia)
                        VALUES (?, ?, CURRENT_DATE, 'Activa')
                        """, Statement.RETURN_GENERATED_KEYS)) {
                    statement.setInt(1, studentId);
                    statement.setInt(2, clubId);
                    statement.executeUpdate();
                    try (var keys = statement.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Club membership ID was not generated.");
                        }
                        id = keys.getInt(1);
                    }
                }
                connection.commit();
                return id;
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    public void withdraw(String document, int membershipId, String reason) throws SQLException {
        if (reason == null || reason.isBlank() || reason.strip().length() > 1000) {
            throw new IllegalArgumentException("Ingresa un motivo de retiro de 1 a 1000 caracteres.");
        }
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                int clubId;
                int studentId;
                try (var statement = connection.prepareStatement("""
                        SELECT mc.id_club, mc.id_estudiante FROM membresias_club mc
                        JOIN estudiantes e ON e.id_estudiante = mc.id_estudiante
                        WHERE mc.id_membresia = ? AND e.numero_documento = ?
                        """)) {
                    statement.setInt(1, membershipId);
                    statement.setString(2, document);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("La membresía no pertenece al estudiante consultado.");
                        }
                        clubId = result.getInt("id_club");
                        studentId = result.getInt("id_estudiante");
                    }
                }
                // Use the same club lock as enrollment before releasing a place.
                try (var statement = connection.prepareStatement(
                        "SELECT id_club FROM clubes WHERE id_club = ? FOR UPDATE")) {
                    statement.setInt(1, clubId);
                    try (var result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new IllegalArgumentException("El club ya no existe.");
                        }
                    }
                }
                try (var statement = connection.prepareStatement("""
                        UPDATE membresias_club
                        SET estado_membresia = 'Retirada', fecha_retiro = CURRENT_DATE, motivo_retiro = ?
                        WHERE id_membresia = ? AND id_estudiante = ? AND id_club = ?
                          AND estado_membresia = 'Activa' AND fecha_ingreso <= CURRENT_DATE
                        """)) {
                    statement.setString(1, reason.strip());
                    statement.setInt(2, membershipId);
                    statement.setInt(3, studentId);
                    statement.setInt(4, clubId);
                    if (statement.executeUpdate() != 1) {
                        throw new IllegalArgumentException("La membresía ya no está activa o su fecha de ingreso es futura.");
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    public List<ClubMembership> findByStudent(String document) throws SQLException {
        String sql = """
                SELECT mc.id_membresia, c.nombre_club, mc.fecha_ingreso,
                       mc.estado_membresia, mc.fecha_retiro
                FROM membresias_club mc
                JOIN clubes c ON c.id_club = mc.id_club
                JOIN estudiantes e ON e.id_estudiante = mc.id_estudiante
                WHERE e.numero_documento = ?
                ORDER BY mc.fecha_ingreso DESC, mc.id_membresia DESC
                """;
        List<ClubMembership> memberships = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, document);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    var withdrawal = result.getDate("fecha_retiro");
                    memberships.add(new ClubMembership(result.getInt("id_membresia"),
                            result.getString("nombre_club"), result.getDate("fecha_ingreso").toLocalDate(),
                            result.getString("estado_membresia"), withdrawal == null ? null : withdrawal.toLocalDate()));
                }
            }
        }
        return memberships;
    }
}
