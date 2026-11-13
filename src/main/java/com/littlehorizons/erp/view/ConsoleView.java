package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.GradingOption;
import java.math.BigDecimal;
import java.util.Map;
import com.littlehorizons.erp.model.Subject;
import com.littlehorizons.erp.model.SubjectRegistration;
import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.model.Enrollment;
import com.littlehorizons.erp.model.Student;
import java.io.PrintStream;
import java.util.List;
import java.util.OptionalInt;
import java.util.Optional;
import java.util.Scanner;

public final class ConsoleView {
    private final Scanner input;
    private final PrintStream output;

    public ConsoleView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void showMainMenu() {
        output.println("""
                ERP Colegio Little Horizons

                1. Gestión de Estudiantes
                2. Gestión Académica
                3. Gestión de Profesores
                4. Actividades Extracurriculares
                5. Gestión Financiera
                6. Gestión de Recursos
                7. Reportes y Estadísticas
                8. Salir
                """);
    }

    public OptionalInt readMenuOption(int min, int max) {
        while (true) {
            output.print("Seleccione una opción: ");
            if (!input.hasNextLine()) {
                return OptionalInt.empty();
            }
            try {
                int option = Integer.parseInt(input.nextLine().trim());
                if (option >= min && option <= max) {
                    return OptionalInt.of(option);
                }
            } catch (NumberFormatException exception) {
                // Invalid input uses the same retry message as an out-of-range option.
            }
            output.printf("Ingrese un número entre %d y %d.%n", min, max);
        }
    }

    public void showStudentMenu() {
        output.println("""
                Gestión de Estudiantes

                1. Listar estudiantes
                2. Registrar con acudiente existente
                3. Registrar acudiente
                4. Consultar grados
                5. Matricular estudiante
                6. Consultar matrículas
                7. Retirar matrícula
                8. Registrar graduación
                9. Reactivar estudiante
                10. Actualizar datos de estudiante
                11. Actualizar datos de acudiente
                0. Volver al menú principal
                """);
    }

    public void showStudentRegistered(int studentId) {
        output.println("Estudiante registrado con su acudiente. ID: " + studentId);
    }

    public void showGuardianRegistered(int guardianId) {
        output.println("Acudiente registrado. ID: " + guardianId);
        output.println("Puedes usar su documento en la opción 2 para vincularlo a un estudiante nuevo.");
    }

    public void showGrades(List<Grade> grades) {
        if (grades.isEmpty()) {
            output.println("No hay grados registrados.");
            return;
        }
        output.println("ID | Grado | Nivel");
        for (Grade grade : grades) {
            output.printf("%d | %s | %d%n", grade.id(), grade.name(), grade.level());
        }
    }

    public Optional<String> readStudentDocument() {
        while (true) {
            output.print("Documento del estudiante, o /volver para cancelar: ");
            if (!input.hasNextLine()) {
                return Optional.empty();
            }
            String document = input.nextLine().strip();
            if (document.equalsIgnoreCase("/volver")) {
                return Optional.empty();
            }
            if (!document.isEmpty() && document.length() <= 20) {
                return Optional.of(document);
            }
            output.println("Ingresa un documento de hasta 20 caracteres.");
        }
    }

    public OptionalInt readGradeId(List<Grade> grades) {
        while (true) {
            output.println("Selecciona el ID del grado. Usa 0 para volver.");
            var selected = readMenuOption(0, Integer.MAX_VALUE);
            if (selected.isEmpty() || selected.getAsInt() == 0) {
                return OptionalInt.empty();
            }
            if (grades.stream().anyMatch(grade -> grade.id() == selected.getAsInt())) {
                return selected;
            }
            output.println("Selecciona un ID que aparezca en el catálogo.");
        }
    }

    public OptionalInt readAcademicYear() {
        while (true) {
            output.println("Año académico de cuatro cifras. Usa 0 para volver.");
            var year = readMenuOption(0, 9999);
            if (year.isEmpty() || year.getAsInt() == 0) {
                return OptionalInt.empty();
            }
            if (year.getAsInt() >= 1000) {
                return year;
            }
            output.println("El año académico debe tener cuatro cifras.");
        }
    }

    public void showEnrollmentRegistered(int id) {
        output.println("Matrícula activa registrada. ID: " + id);
    }

    public void showEnrollments(List<Enrollment> enrollments) {
        if (enrollments.isEmpty()) {
            output.println("No hay matrículas registradas.");
            return;
        }
        output.println("ID | Estudiante | Documento | Grado | Año | Estado | Fecha");
        for (Enrollment enrollment : enrollments) {
            output.printf("%d | %s | %s | %s | %d | %s | %s%n",
                    enrollment.id(), enrollment.studentName(), enrollment.documentNumber(),
                    enrollment.gradeName(), enrollment.academicYear(),
                    enrollment.status(), enrollment.enrollmentDate());
        }
    }

    public void showAcademicMenu() {
        output.println("""
                Gestión Académica

                1. Inscribir estudiante en materia
                2. Consultar inscripciones de estudiante
                3. Consultar historial de notas y promedios
                4. Registrar calificación
                5. Registrar asistencia
                6. Consultar historial de asistencia
                7. Reporte semanal de inasistencias
                0. Volver al menú principal
                """);
    }

    public OptionalInt readSubjectId(List<Subject> subjects) {
        for (Subject subject : subjects) {
            output.printf("%d | %s | %s%n", subject.id(), subject.code(), subject.name());
        }
        while (true) {
            output.println("Selecciona el ID de la materia. Usa 0 para volver.");
            var id = readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return OptionalInt.empty();
            }
            if (subjects.stream().anyMatch(subject -> subject.id() == id.getAsInt())) {
                return id;
            }
            output.println("Selecciona una materia del listado.");
        }
    }

    public Optional<String> readAcademicPeriod() {
        return readPeriod(List.of("I", "II", "III", "IV"));
    }

    public Optional<String> readGradingPeriod() {
        return readPeriod(List.of("I", "II", "III", "IV", "Final"));
    }

    private Optional<String> readPeriod(List<String> periods) {
        while (true) {
            output.print("Período (" + String.join(", ", periods) + "). Usa /volver para cancelar: ");
            if (!input.hasNextLine()) {
                return Optional.empty();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                return Optional.empty();
            }
            for (String period : periods) {
                if (period.equalsIgnoreCase(value)) {
                    return Optional.of(period);
                }
            }
            output.println("Ingresa uno de los períodos indicados.");
        }
    }

    public Optional<String> readSection() {
        while (true) {
            output.print("Sección opcional, Enter para omitir o /volver para cancelar: ");
            if (!input.hasNextLine()) {
                return Optional.empty();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                return Optional.empty();
            }
            if (value.length() <= 10) {
                return Optional.of(value);
            }
            output.println("La sección admite hasta 10 caracteres.");
        }
    }

    public void showSubjectRegistered(int id) {
        output.println("Inscripción activa registrada. ID: " + id);
    }

    public void showSubjectRegistrations(List<SubjectRegistration> registrations) {
        if (registrations.isEmpty()) {
            output.println("No se encontraron inscripciones para ese documento.");
            return;
        }
        output.println("Materia | Año | Período | Sección | Estado");
        for (SubjectRegistration registration : registrations) {
            output.printf("%s | %d | %s | %s | %s%n", registration.subjectName(),
                    registration.year(), registration.period(),
                    registration.section() == null ? "Sin sección" : registration.section(),
                    registration.status());
        }
    }

    public Optional<GradingOption> readGradingOption(List<GradingOption> options) {
        for (int index = 0; index < options.size(); index++) {
            var option = options.get(index);
            output.printf("%d. %s — %s%n", index + 1, option.subjectName(), option.teacherName());
        }
        output.println("Selecciona materia y profesor. Usa 0 para volver.");
        var selected = readMenuOption(0, options.size());
        if (selected.isEmpty() || selected.getAsInt() == 0) {
            return Optional.empty();
        }
        return Optional.of(options.get(selected.getAsInt() - 1));
    }

    public Optional<BigDecimal> readGrade() {
        while (true) {
            output.print("Nota entre 0,0 y 5,0. Usa /volver para cancelar: ");
            if (!input.hasNextLine()) {
                return Optional.empty();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                return Optional.empty();
            }
            if (value.matches("[0-5]([.,][0-9])?")) {
                BigDecimal grade = new BigDecimal(value.replace(',', '.'));
                if (grade.compareTo(new BigDecimal("5.0")) <= 0) {
                    return Optional.of(grade);
                }
            }
            output.println("Ingresa una nota de 0 a 5 con máximo un decimal.");
        }
    }

    public void showGradeRegistered(int id) {
        output.println("Calificación registrada. ID: " + id);
    }

    public void showAcademicRecords(List<AcademicRecord> records) {
        if (records.isEmpty()) {
            output.println("No se encontraron calificaciones para ese documento.");
            return;
        }
        output.println("Año | Período | Materia | Nota | Fecha");
        for (AcademicRecord record : records) {
            output.printf("%d | %s | %s | %s | %s%n", record.year(), record.period(),
                    record.subjectName(), record.grade().toPlainString(), record.registrationDate());
        }
    }

    public void showAcademicAverages(Map<Integer, Map<String, BigDecimal>> averages) {
        averages.forEach((year, periods) -> periods.forEach((period, average) ->
                output.printf("Promedio %d, período %s: %s%n", year, period, average.toPlainString())));
    }

    public void showStudents(List<Student> students) {
        output.println("Estudiantes registrados: " + students.size());
        if (students.isEmpty()) {
            output.println("No hay estudiantes registrados.");
            return;
        }

        for (Student student : students) {
            String status = student.status() == null ? "Sin estado" : student.status();
            output.printf("%s | %s %s | %s%n",
                    student.fullName(), student.documentType(), student.documentNumber(), status);
        }
    }

    public void showError(String message) {
        output.println(message);
    }

    public void showModulePending() {
        output.println("Este módulo está pendiente de implementación.");
    }

    public void showGoodbye() {
        output.println("Hasta pronto.");
    }
}
