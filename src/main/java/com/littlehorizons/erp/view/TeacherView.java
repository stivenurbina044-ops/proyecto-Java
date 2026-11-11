package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Teacher;
import com.littlehorizons.erp.model.TeacherAssignment;
import java.io.PrintStream;
import com.littlehorizons.erp.model.TeacherRegistration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;
import com.littlehorizons.erp.model.ClassSchedule;
import com.littlehorizons.erp.model.ClassScheduleRegistration;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.List;

public final class TeacherView {
    private final PrintStream output;
    private final Scanner input;
    private static final Pattern NAME = Pattern.compile("\\p{L}[\\p{L}\\p{M}]*(?:[ '’-]\\p{L}[\\p{L}\\p{M}]*)*");
    private static final Pattern EMAIL = Pattern.compile("[^\\s@]+@[^\\s@.]+(?:\\.[^\\s@.]+)+");

    public TeacherView(Scanner input, PrintStream output) {
        this.output = output;
        this.input = input;
    }

    public void showMenu() {
        output.println("\nGestión de Profesores");
        output.println("1. Listar profesores");
        output.println("2. Registrar profesor");
        output.println("3. Asignar materia");
        output.println("4. Consultar asignaciones");
        output.println("5. Registrar horario");
        output.println("6. Consultar horarios");
        output.println("0. Volver al menú principal");
    }

    public void showTeachers(List<Teacher> teachers) {
        output.println("Profesores registrados: " + teachers.size());
        if (teachers.isEmpty()) {
            output.println("No hay profesores registrados.");
            return;
        }
        output.printf("%-6s %-20s %-45s %s%n", "ID", "Documento", "Nombre completo", "Estado");
        for (Teacher teacher : teachers) {
            output.printf("%-6d %-20s %-45s %s%n", teacher.id(), teacher.documentNumber(),
                    teacher.fullName(), teacher.status());
        }
    }
    public void showAssignments(List<TeacherAssignment> assignments) {
        output.println("Asignaciones registradas: " + assignments.size());
        if (assignments.isEmpty()) {
            output.println("No hay asignaciones registradas.");
            return;
        }
        for (TeacherAssignment assignment : assignments) {
            String section = assignment.section();
            output.printf("%d | %s | %s - %s | Año: %d | Sección: %s%n",
                    assignment.id(), assignment.teacherName(), assignment.subjectCode(),
                    assignment.subjectName(), assignment.year(),
                    section == null || section.isBlank() ? "Sin sección" : section);
        }
    }

    public void showScheduleAssignmentPrompt() {
        output.println("Selecciona el ID de la asignación del listado. Usa 0 para volver.");
    }

    public Optional<ClassScheduleRegistration> readSchedule() {
        output.println("Horario semanal. Escribe /volver para cancelar.");
        try {
            int day;
            while (true) {
                String value = readText("Día (1=Lunes, 2=Martes, 3=Miércoles, 4=Jueves, 5=Viernes, 6=Sábado, 7=Domingo)", false, 1);
                if (value.matches("[1-7]")) {
                    day = Integer.parseInt(value);
                    break;
                }
                output.println("Ingresa un número entre 1 y 7.");
            }
            LocalTime start = readTime("Hora de inicio");
            while (true) {
                LocalTime end = readTime("Hora de fin");
                if (start.isBefore(end)) {
                    return Optional.of(new ClassScheduleRegistration(day, start, end));
                }
                output.println("La hora de fin debe ser posterior al inicio y del mismo día.");
            }
        } catch (RegistrationCancelled exception) {
            output.println("Registro cancelado.");
            return Optional.empty();
        }
    }

    private LocalTime readTime(String prompt) {
        while (true) {
            String value = readText(prompt + " (HH:mm, formato de 24 horas)", false, 5);
            if (value.matches("(?:[01][0-9]|2[0-3]):[0-5][0-9]")) {
                return LocalTime.parse(value);
            }
            output.println("Ingresa una hora válida de 00:00 a 23:59, por ejemplo 08:30.");
        }
    }

    public void showScheduleRegistered(int id) {
        output.println("Horario registrado correctamente. ID: " + id);
    }

    public void showSchedules(List<ClassSchedule> schedules) {
        output.println("Horarios registrados: " + schedules.size());
        if (schedules.isEmpty()) {
            output.println("No hay horarios registrados.");
        }
        for (ClassSchedule schedule : schedules) {
            String day = DayOfWeek.of(schedule.dayOfWeek()).getDisplayName(TextStyle.FULL,
                    Locale.forLanguageTag("es-CO"));
            output.printf("%d | Asignación: %d | %s | %s | Año: %d | Sección: %s | %s %s–%s%n",
                    schedule.id(), schedule.assignmentId(), schedule.teacherName(), schedule.subjectCode(),
                    schedule.year(), schedule.section() == null ? "Sin sección" : schedule.section(),
                    day, schedule.startTime(), schedule.endTime());
        }
    }

    public void showAssignmentPrompt() {
        output.println("Selecciona el ID del profesor del listado. Usa 0 para volver.");
    }

    public void showAssignmentRegistered(int id) {
        output.println("Asignación registrada correctamente. ID: " + id);
    }

    public void showRegistered(int id) {
        output.println("Profesor registrado correctamente. ID: " + id);
    }

    public Optional<TeacherRegistration> readRegistration() {
        output.println("Registro de profesor. Escribe /volver para cancelar.");
        try {
            String type;
            while (true) {
                String value = readText("Tipo de documento (CC, CE, Pasaporte)", false, 20);
                type = List.of("CC", "CE", "Pasaporte").stream()
                        .filter(choice -> choice.equalsIgnoreCase(value)).findFirst().orElse(null);
                if (type != null) {
                    break;
                }
                output.println("Ingresa CC, CE o Pasaporte.");
            }
            String document = readMatching("Número de documento", false, 20,
                    Pattern.compile(type.equals("Pasaporte") ? "[A-Za-z0-9]{2,20}" : "[0-9]{2,20}"),
                    "Documento inválido. Usa de 2 a 20 dígitos; el pasaporte admite letras.");
            String firstName = readMatching("Primer nombre", false, 100, NAME, "Usa letras para el nombre.");
            String middleName = readMatching("Segundo nombre (opcional)", true, 100, NAME, "Usa letras para el nombre.");
            String lastName = readMatching("Primer apellido", false, 100, NAME, "Usa letras para el apellido.");
            String secondLastName = readMatching("Segundo apellido (opcional)", true, 100, NAME, "Usa letras para el apellido.");
            String email = readMatching("Correo electrónico", false, 100, EMAIL, "Ingresa un correo válido.");
            String phone = readMatching("Teléfono (opcional)", true, 20,
                    Pattern.compile("[+]?[0-9]{7,15}"), "Usa de 7 a 15 dígitos y un + inicial opcional.");
            String specialization = readText("Especialización (opcional)", true, 100);
            LocalDate hireDate = readHireDate();
            return Optional.of(new TeacherRegistration(type, document, firstName, middleName,
                    lastName, secondLastName, email, phone, specialization, hireDate));
        } catch (RegistrationCancelled exception) {
            output.println("Registro cancelado.");
            return Optional.empty();
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
                return value.isEmpty() ? null : value;
            }
            output.printf("Ingresa %s de máximo %d caracteres.%n",
                    optional ? "un valor" : "un valor obligatorio", maxLength);
        }
    }

    private String readMatching(String prompt, boolean optional, int maxLength,
                                Pattern pattern, String error) {
        while (true) {
            String value = readText(prompt, optional, maxLength);
            if (value == null || pattern.matcher(value).matches()) {
                return value;
            }
            output.println(error);
        }
    }

    private LocalDate readHireDate() {
        while (true) {
            String value = readText("Fecha de contratación (AAAA-MM-DD)", false, 10);
            try {
                LocalDate date = LocalDate.parse(value);
                if (date.getYear() >= 1000 && date.getYear() <= 9999) {
                    return date;
                }
            } catch (DateTimeParseException exception) {
                // Invalid dates are retried without leaving the form.
            }
            output.println("Ingresa una fecha válida con año de cuatro cifras (AAAA-MM-DD).");
        }
    }

    private static final class RegistrationCancelled extends RuntimeException {
    }
}
