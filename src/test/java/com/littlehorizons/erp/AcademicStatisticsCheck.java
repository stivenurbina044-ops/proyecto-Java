package com.littlehorizons.erp;

import com.littlehorizons.erp.model.ReportGrade;
import com.littlehorizons.erp.service.AcademicStatisticsService;
import com.littlehorizons.erp.view.ReportView;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.List;

public final class AcademicStatisticsCheck {
    private static ReportGrade grade(int student, int subject, int year, String period, String value) {
        return new ReportGrade(student, subject, "MAT-" + subject, "Matemáticas", year, period, new BigDecimal(value));
    }

    public static void main(String[] args) {
        var service = new AcademicStatisticsService();
        var student = service.studentSubjectAverages(List.of(
                grade(1, 1, 2026, "I", "3.0"), grade(1, 1, 2026, "II", "4.1"),
                grade(1, 1, 2026, "Final", "5.0"), grade(1, 2, 2026, "I", "2.0"),
                grade(1, 1, 2025, "I", "1.0"), grade(1, 3, 2026, "Final", "4.5")));
        require(student.size() == 4, "Subjects or years were merged");
        var math = student.stream().filter(value -> value.subjectId() == 1 && value.year() == 2026).findFirst().orElseThrow();
        require(math.recordedPeriods() == 2 && math.periodAverage().equals(new BigDecimal("3.6")),
                "Incorrect rounding, Final included, or missing periods counted as zero");
        require(math.finalGrade().equals(new BigDecimal("5.0")), "Manual Final grade changed");
        var finalOnly = student.stream().filter(value -> value.subjectId() == 3).findFirst().orElseThrow();
        require(finalOnly.periodAverage() == null && finalOnly.recordedPeriods() == 0, "Final-only average fabricated");
        var course = service.courseSubjectAverages(List.of(
                grade(1, 1, 2026, "I", "3.0"), grade(2, 1, 2026, "I", "4.1"),
                grade(1, 1, 2026, "II", "2.0"), grade(1, 1, 2025, "I", "5.0"),
                grade(1, 2, 2026, "I", "1.0"), grade(1, 1, 2026, "Final", "4.0")));
        require(course.size() == 5, "Course subjects, periods or years were merged");
        var group = course.stream().filter(value -> value.subjectId() == 1 && value.year() == 2026
                && value.period().equals("I")).findFirst().orElseThrow();
        require(group.gradedStudents() == 2 && group.average().equals(new BigDecimal("3.6")), "Incorrect course average");
        require(service.studentSubjectAverages(List.of()).isEmpty() && service.courseSubjectAverages(List.of()).isEmpty(),
                "Empty input should yield empty reports");
        var buffer = new ByteArrayOutputStream();
        var view = new ReportView(new PrintStream(buffer));
        view.showStudentAverages("123", 2026, student);
        view.showCourseAverages(1, 2026, "I", course);
        view.showStudentAverages("123", 2026, List.of());
        require(buffer.toString().contains("Sin registrar") && buffer.toString().contains("No hay calificaciones"),
                "Missing-grade display failed");
        System.out.println("Academic statistics checks passed: rounding, year/subject/period isolation, Final separation and missing grades.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
