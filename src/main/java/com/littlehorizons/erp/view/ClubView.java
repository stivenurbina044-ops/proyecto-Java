package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Club;
import com.littlehorizons.erp.model.ClubMembership;
import com.littlehorizons.erp.model.ClubRegistration;
import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public final class ClubView {
    private final Scanner input;
    private final PrintStream output;

    public ClubView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void showMenu() {
        output.println("""
                Actividades Extracurriculares
                1. Listar clubes
                2. Registrar club
                3. Inscribir estudiante en club
                4. Consultar clubes de estudiante
                5. Retirar estudiante de club
                0. Volver al menú principal
                """);
    }

    public void showClubs(List<Club> clubs) {
        output.println("Clubes encontrados: " + clubs.size());
        if (clubs.isEmpty()) {
            output.println("No hay clubes disponibles para esta consulta.");
        }
        for (Club club : clubs) {
            String capacity = club.capacity() == null || club.capacity() <= 0
                    ? "Sin configurar" : club.capacity().toString();
            String places = club.capacity() == null || club.capacity() <= 0 ? "Sin configurar"
                    : Integer.toString(Math.max(0, club.capacity() - club.activeMembers()));
            output.printf("%d | %s | %s | Encargado: %s | Miembros: %d | Cupo máximo: %s | Disponibles: %s%n",
                    club.id(), club.name(), club.active() ? "Activo" : "Inactivo",
                    displayOptional(club.supervisorName()), club.activeMembers(), capacity, places);
            output.printf("Descripción: %s | Reunión: %s%n",
                    displayOptional(club.description()), displayOptional(club.meetingSchedule()));
        }
    }

    public Optional<ClubRegistration> readRegistration(int supervisorId) {
        output.println("Registro de club. Escribe /volver para cancelar.");
        try {
            String name = readText("Nombre del club", false, 100);
            String description = readText("Descripción (opcional)", true, 1000);
            int capacity;
            while (true) {
                String value = readText("Cupo máximo (entero mayor que cero)", false, 10);
                try {
                    if (value.matches("[0-9]+") && Integer.parseInt(value) > 0) {
                        capacity = Integer.parseInt(value);
                        break;
                    }
                } catch (NumberFormatException exception) {
                    // Retry values outside the integer range.
                }
                output.println("Ingresa un entero mayor que cero, máximo 2147483647.");
            }
            String schedule = readText("Días y horario de reunión (opcional)", true, 100);
            return Optional.of(new ClubRegistration(name, description, supervisorId, capacity, schedule));
        } catch (RegistrationCancelled exception) {
            output.println("Registro cancelado.");
            return Optional.empty();
        }
    }

    public Optional<String> readWithdrawalReason() {
        output.println("Retiro de club. Escribe /volver para cancelar.");
        try {
            return Optional.of(readText("Motivo del retiro (obligatorio, máximo 1000 caracteres)", false, 1000));
        } catch (RegistrationCancelled exception) {
            output.println("Retiro cancelado.");
            return Optional.empty();
        }
    }

    public void showWithdrawn() {
        output.println("Retiro registrado correctamente. El historial se conserva y el cupo queda disponible.");
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

    public void showRegistered(int id) {
        output.println("Club registrado correctamente. ID: " + id);
    }

    public void showEnrolled(int id) {
        output.println("Estudiante inscrito correctamente en el club. ID de membresía: " + id);
    }

    public void showMemberships(List<ClubMembership> memberships) {
        output.println("Membresías encontradas: " + memberships.size());
        if (memberships.isEmpty()) {
            output.println("No hay membresías para el documento consultado.");
        }
        for (ClubMembership membership : memberships) {
            output.printf("%d | %s | Ingreso: %s | Estado: %s | Retiro: %s%n", membership.id(),
                    membership.clubName(), membership.entryDate(), membership.status(),
                    membership.withdrawalDate() == null ? "Sin retiro" : membership.withdrawalDate());
        }
    }

    private String displayOptional(String value) {
        return value == null || value.isBlank() ? "Sin registrar" : value;
    }

    private static final class RegistrationCancelled extends RuntimeException {
    }
}
