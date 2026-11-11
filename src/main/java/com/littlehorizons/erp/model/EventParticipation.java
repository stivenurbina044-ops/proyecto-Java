package com.littlehorizons.erp.model;

import java.util.List;

public record EventParticipation(int id, String documentNumber, String studentName,
        String role, String position, String notes) {
    public static final List<String> ROLES = List.of("Participante", "Competidor", "Organizador", "Asistente");
    public static final List<String> POSITIONS = List.of("1er Lugar", "2do Lugar", "3er Lugar", "Participante");
}
