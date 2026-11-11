package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventRegistration;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.sql.Date;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class EventDao {
    public List<SchoolEvent> findAll() throws SQLException {
        String sql = """
                SELECT v.id_evento, v.nombre_evento, v.descripcion, v.tipo_evento, v.fecha_evento,
                       v.lugar, v.tiene_competencia, v.esta_activo,
                       CONCAT_WS(' ', p.primer_nombre, NULLIF(p.segundo_nombre, ''),
                                 p.primer_apellido, NULLIF(p.segundo_apellido, '')) AS organizador
                FROM eventos v LEFT JOIN profesores p ON p.id_profesor = v.id_organizador
                ORDER BY v.fecha_evento, v.id_evento
                """;
        List<SchoolEvent> events = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql);
             var result = statement.executeQuery()) {
            while (result.next()) {
                events.add(new SchoolEvent(result.getInt("id_evento"), result.getString("nombre_evento"),
                        result.getString("descripcion"), result.getString("tipo_evento"),
                        result.getDate("fecha_evento").toLocalDate(), result.getString("lugar"),
                        result.getBoolean("tiene_competencia"), result.getString("organizador"),
                        result.getBoolean("esta_activo")));
            }
        }
        return events;
    }

    public int insert(EventRegistration event) throws SQLException {
        String sql = """
                INSERT INTO eventos (nombre_evento, descripcion, tipo_evento, fecha_evento,
                                     lugar, tiene_competencia, id_organizador, esta_activo)
                SELECT ?, ?, ?, ?, ?, ?, p.id_profesor, TRUE FROM profesores p
                WHERE p.id_profesor = ? AND p.estado = 'Activo'
                  AND p.fecha_contratacion <= CURRENT_DATE AND p.fecha_contratacion <= ?
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, event.name());
            statement.setString(2, event.description().isEmpty() ? null : event.description());
            statement.setString(3, event.type());
            statement.setDate(4, Date.valueOf(event.date()));
            statement.setString(5, event.location().isEmpty() ? null : event.location());
            statement.setBoolean(6, event.competition());
            statement.setInt(7, event.organizerId());
            statement.setDate(8, Date.valueOf(event.date()));
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("El organizador no está activo o su contratación es posterior a hoy o al evento.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Event ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public int registerParticipant(int eventId, String document, String role, String notes) throws SQLException {
        if (role == null || !EventParticipation.ROLES.contains(role) || notes == null || notes.length() > 1000) {
            throw new IllegalArgumentException("Rol u observaciones de participación inválidos.");
        }
        String sql = """
                INSERT INTO participantes_evento (id_evento, id_estudiante, rol_participacion, observaciones)
                SELECT v.id_evento, e.id_estudiante, ?, ?
                FROM eventos v JOIN estudiantes e ON e.numero_documento = ?
                WHERE v.id_evento = ? AND v.esta_activo = TRUE AND e.estado = 'Activo'
                  AND e.fecha_ingreso <= v.fecha_evento
                  AND (? <> 'Competidor' OR v.tiene_competencia = TRUE)
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, role);
            statement.setString(2, notes.isBlank() ? null : notes.strip());
            statement.setString(3, document);
            statement.setInt(4, eventId);
            statement.setString(5, role);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Revisa el evento activo, estudiante activo, fecha de ingreso y rol permitido.");
            }
            try (var keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Event participation ID was not generated.");
                }
                return keys.getInt(1);
            }
        }
    }

    public List<EventParticipation> findParticipants(int eventId) throws SQLException {
        String sql = """
                SELECT pe.id_participacion, e.numero_documento, pe.rol_participacion,
                       pe.posicion_obtenida, pe.observaciones,
                       CONCAT_WS(' ', e.primer_nombre, NULLIF(e.segundo_nombre, ''),
                                 e.primer_apellido, NULLIF(e.segundo_apellido, '')) AS estudiante
                FROM participantes_evento pe
                JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante
                WHERE pe.id_evento = ?
                ORDER BY e.primer_apellido, e.primer_nombre, pe.id_participacion
                """;
        List<EventParticipation> participants = new ArrayList<>();
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setInt(1, eventId);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    participants.add(new EventParticipation(result.getInt("id_participacion"),
                            result.getString("numero_documento"), result.getString("estudiante"),
                            result.getString("rol_participacion"), result.getString("posicion_obtenida"),
                            result.getString("observaciones")));
                }
            }
        }
        return participants;
    }

    public void recordPosition(int eventId, int participationId, String position) throws SQLException {
        if (position == null || !EventParticipation.POSITIONS.contains(position)) {
            throw new IllegalArgumentException("Selecciona una posición válida.");
        }
        String sql = """
                UPDATE participantes_evento pe JOIN eventos v ON v.id_evento = pe.id_evento
                SET pe.posicion_obtenida = ?
                WHERE pe.id_participacion = ? AND pe.id_evento = ? AND pe.rol_participacion = 'Competidor'
                  AND v.esta_activo = TRUE AND v.tiene_competencia = TRUE AND v.fecha_evento <= CURRENT_DATE
                """;
        try (var connection = DatabaseConnection.open();
             var statement = connection.prepareStatement(sql)) {
            statement.setString(1, position);
            statement.setInt(2, participationId);
            statement.setInt(3, eventId);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("No se actualizó el resultado. Revisa la participación, competencia, fecha y estado del evento, o si ya tenía esa posición.");
            }
        }
    }
}
