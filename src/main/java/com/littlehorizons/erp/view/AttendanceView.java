package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Attendance;
import com.littlehorizons.erp.model.WeeklyAbsence;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public final class AttendanceView {
    private final Scanner input;
    private final PrintStream output;

    public AttendanceView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public Optional<LocalDate> readDate() {
        return readDate("Fecha de asistencia (AAAA-MM-DD)");
    }

    public Optional<LocalDate> readReportDate() {
        output.println("Reporte de más de 3 días con ausencia. Semana de lunes a domingo.");
        return readDate("Fecha de la semana que deseas consultar (AAAA-MM-DD)");
    }

    private Optional<LocalDate> readDate(String prompt) {
        while (true) {
            var value = readText(prompt, false, 10);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            try {
                LocalDate date = LocalDate.parse(value.get());
                if (date.getYear() >= 1000 && !date.isAfter(LocalDate.now())) {
                    return Optional.of(date);
                }
            } catch (DateTimeParseException exception) {
                // Invalid dates are retried without leaving the form.
            }
            output.println("Ingresa una fecha válida, con año de cuatro cifras, que no sea futura.");
        }
    }

    public Optional<String> readStatus() {
        List<String> statuses = List.of("Presente", "Ausente", "Tardanza", "Excusa Médica");
        output.println("1. Presente  2. Ausente  3. Tardanza  4. Excusa Médica");
        while (true) {
            var value = readText("Estado (1-4)", false, 1);
            if (value.isEmpty()) {
                return Optional.empty();
            }
            if (value.get().matches("[1-4]")) {
                return Optional.of(statuses.get(Integer.parseInt(value.get()) - 1));
            }
            output.println("Selecciona un estado entre 1 y 4.");
        }
    }

    public Optional<String> readNotes() {
        return readText("Observaciones opcionales (Enter para omitir)", true, 1000);
    }

    private Optional<String> readText(String prompt, boolean optional, int maxLength) {
        while (true) {
            output.print(prompt + ", o /volver para cancelar: ");
            if (!input.hasNextLine()) {
                return Optional.empty();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                return Optional.empty();
            }
            if ((optional || !value.isEmpty()) && value.length() <= maxLength) {
                return Optional.of(value);
            }
            output.printf("Ingresa un valor %sde máximo %d caracteres.%n",
                    optional ? "" : "obligatorio ", maxLength);
        }
    }

    public void showWeeklyAbsences(LocalDate referenceDate, List<WeeklyAbsence> report) {
        LocalDate monday = referenceDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        output.printf("Reporte semanal: %s a %s%n", monday, monday.plusDays(6));
        output.println("Criterio: más de 3 días distintos con estado Ausente.");
        if (report.isEmpty()) {
            output.println("No hay estudiantes con más de 3 días de ausencia registrados en esta semana.");
            return;
        }
        output.printf("%-6s %-20s %-45s %s%n", "ID", "Documento", "Estudiante", "Días con ausencia");
        for (WeeklyAbsence absence : report) {
            output.printf("%-6d %-20s %-45s %d%n", absence.studentId(), absence.documentNumber(),
                    absence.studentName(), absence.absentDays());
        }
    }

    public void showRegistered(int id) {
        output.println("Asistencia registrada correctamente. ID: " + id);
    }

    public void showHistory(List<Attendance> records) {
        output.println("Registros de asistencia: " + records.size());
        if (records.isEmpty()) {
            output.println("No hay asistencias para el documento consultado.");
        }
        for (Attendance record : records) {
            String teacher = record.teacherName();
            output.printf("%d | %s | %s | %s | Profesor: %s | Observaciones: %s%n",
                    record.id(), record.date(), record.subjectName(), record.status(),
                    teacher == null || teacher.isBlank() ? "Sin registrar" : teacher,
                    record.notes() == null || record.notes().isBlank() ? "Sin observaciones" : record.notes());
        }
    }
}
