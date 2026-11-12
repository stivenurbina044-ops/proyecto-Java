package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.CourseSubjectAverage;
import com.littlehorizons.erp.model.StudentSubjectAverage;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

public final class ReportView {
    private final PrintStream output;

    public ReportView(PrintStream output) {
        this.output = output;
    }

    public void showMenu() {
        output.println("""
                Reportes y Estadísticas
                1. Promedios por materia de estudiante
                2. Promedio del grado por materia y período
                0. Volver al menú principal
                """);
    }

    public void showStudentAverages(String document, int year, List<StudentSubjectAverage> averages) {
        output.printf("Documento: %s | Año: %d%n", document, year);
        output.println("Media simple de períodos registrados I–IV. La nota Final manual se muestra separada.");
        if (averages.isEmpty()) {
            output.println("No hay calificaciones para ese documento y año.");
        }
        for (StudentSubjectAverage average : averages) {
            output.printf("%s | %s | Períodos con nota: %d de 4 | Promedio: %s | Final manual: %s%n",
                    average.subjectCode(), average.subjectName(), average.recordedPeriods(),
                    format(average.periodAverage()), format(average.finalGrade()));
        }
    }

    public void showCourseAverages(int gradeId, int year, String period, List<CourseSubjectAverage> averages) {
        output.printf("ID del grado: %d | Año: %d | Período: %s | Todas las secciones%n", gradeId, year, period);
        output.println("Solo estudiantes con nota registrada y matrícula en ese grado y año; faltantes no cuentan como cero.");
        if (averages.isEmpty()) {
            output.println("No hay calificaciones para ese grado, año y período.");
        }
        for (CourseSubjectAverage average : averages) {
            output.printf("%s | %s | Estudiantes con nota: %d | Promedio: %s%n", average.subjectCode(),
                    average.subjectName(), average.gradedStudents(), format(average.average()));
        }
    }

    private String format(BigDecimal grade) {
        return grade == null ? "Sin registrar" : grade.toPlainString();
    }
}
