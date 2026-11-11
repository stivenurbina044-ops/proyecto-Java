package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.EventRegistration;
import com.littlehorizons.erp.model.EventParticipation;
import com.littlehorizons.erp.model.SchoolEvent;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public final class EventView {
    private final Scanner input;
    private final PrintStream output;

    public EventView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void showMenu() {
        output.println("""
                Gestión de Eventos
                1. Consultar calendario de eventos
                2. Registrar evento
                3. Registrar participante
                4. Consultar participantes de evento
                5. Registrar o actualizar resultado de competencia
                0. Volver a Actividades Extracurriculares
                """);
    }

    public void showEvents(List<SchoolEvent> events) {
        output.println("Eventos encontrados: " + events.size());
        if (events.isEmpty()) {
            output.println("No hay eventos disponibles para esta consulta.");
        }
        for (SchoolEvent event : events) {
            output.printf("%d | %s | %s | %s | Competencia: %s | %s%n", event.id(), event.name(),
                    event.date(), event.type(), event.competition() ? "Sí" : "No", event.active() ? "Activo" : "Inactivo");
            output.printf("Lugar: %s | Organizador: %s | Descripción: %s%n",
                    displayOptional(event.location()), displayOptional(event.organizerName()), displayOptional(event.description()));
        }
    }

    public Optional<EventRegistration> readRegistration(int organizerId) {
        output.println("Registro de evento. Escribe /volver para cancelar.");
        try {
            String name = readText("Nombre del evento", false, 150);
            String description = readText("Descripción (opcional)", true, 1000);
            String type = readChoice("Tipo de evento", EventRegistration.TYPES);
            LocalDate date;
            while (true) {
                String value = readText("Fecha del evento (AAAA-MM-DD)", false, 10);
                try {
                    date = LocalDate.parse(value);
                    if (date.getYear() >= 1000 && date.getYear() <= 9999) {
                        break;
                    }
                } catch (DateTimeParseException exception) {
                    // Retry invalid calendar dates.
                }
                output.println("Ingresa una fecha válida con año de cuatro cifras.");
            }
            String location = readText("Lugar (opcional)", true, 150);
            boolean competition = readChoice("¿Tiene competencia?", List.of("Sí", "No")).equals("Sí");
            return Optional.of(new EventRegistration(name, description, type, date, location, competition, organizerId));
        } catch (RegistrationCancelled exception) {
            output.println("Registro cancelado.");
            return Optional.empty();
        }
    }

    public Optional<String> readRole(boolean competition) {
        try {
            return Optional.of(readChoice("Rol de participación", competition ? EventParticipation.ROLES
                    : List.of("Participante", "Organizador", "Asistente")));
        } catch (RegistrationCancelled exception) {
            return Optional.empty();
        }
    }

    public Optional<String> readNotes() {
        try {
            return Optional.of(readText("Observaciones (opcional, /volver para cancelar)", true, 1000));
        } catch (RegistrationCancelled exception) {
            return Optional.empty();
        }
    }

    public Optional<String> readPosition() {
        try {
            return Optional.of(readChoice("Posición obtenida", EventParticipation.POSITIONS));
        } catch (RegistrationCancelled exception) {
            return Optional.empty();
        }
    }

    private String readChoice(String prompt, List<String> choices) {
        output.println(prompt + " (0 para volver):");
        for (int index = 0; index < choices.size(); index++) {
            output.printf("%d. %s%n", index + 1, choices.get(index));
        }
        while (true) {
            String value = readText("Selecciona una opción (/volver para cancelar)", false, 10);
            if (value.equals("0")) {
                throw new RegistrationCancelled();
            }
            try {
                int selected = Integer.parseInt(value);
                if (selected >= 1 && selected <= choices.size()) {
                    return choices.get(selected - 1);
                }
            } catch (NumberFormatException exception) {
                // Retry nonnumeric menu entries.
            }
            output.println("Selecciona una opción del listado.");
        }
    }

    private String readText(String prompt, boolean optional, int maxLength) {
        while (true) {
            output.print(prompt + ": ");
            if (!input.hasNextLine()) {
                throw new RegistrationCancelled();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                throw new RegistrationCancelled();
            }
            if ((optional || !value.isEmpty()) && value.length() <= maxLength) {
                return value;
            }
            output.printf("Ingresa un valor %sde máximo %d caracteres.%n",
                    optional ? "" : "obligatorio ", maxLength);
        }
    }

    public void showSelectionPrompt(String entity) {
        output.println("Selecciona el ID de " + entity + " del listado. Usa 0 para volver.");
    }

    public void showSuccess(String operation, int id) {
        output.printf("%s correctamente. ID: %d%n", operation, id);
    }

    public void showParticipants(List<EventParticipation> participants) {
        output.println("Participantes encontrados: " + participants.size());
        if (participants.isEmpty()) {
            output.println("No hay participantes disponibles para esta consulta.");
        }
        for (EventParticipation participant : participants) {
            output.printf("%d | %s | %s | Rol: %s | Resultado: %s | Observaciones: %s%n",
                    participant.id(), participant.documentNumber(), participant.studentName(), participant.role(),
                    participant.position() == null ? "Sin registrar" : participant.position(), displayOptional(participant.notes()));
        }
    }

    private String displayOptional(String value) {
        return value == null || value.isBlank() ? "Sin registrar" : value;
    }

    private static final class RegistrationCancelled extends RuntimeException {
    }
}
