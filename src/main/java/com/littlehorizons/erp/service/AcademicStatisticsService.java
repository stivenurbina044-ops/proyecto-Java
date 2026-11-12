package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.CourseSubjectAverage;
import com.littlehorizons.erp.model.ReportGrade;
import com.littlehorizons.erp.model.StudentSubjectAverage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public final class AcademicStatisticsService {
    private record SubjectKey(int id, String code, String name, int year) { }
    private record CourseKey(SubjectKey subject, String period) { }

    public List<StudentSubjectAverage> studentSubjectAverages(List<ReportGrade> grades) {
        var grouped = grades.stream().collect(Collectors.groupingBy(this::subjectKey));
        return grouped.entrySet().stream().map(entry -> {
            var key = entry.getKey();
            var periodGrades = entry.getValue().stream().filter(grade -> !"Final".equals(grade.period())).toList();
            BigDecimal finalGrade = entry.getValue().stream().filter(grade -> "Final".equals(grade.period()))
                    .map(ReportGrade::value).findFirst().orElse(null);
            return new StudentSubjectAverage(key.id(), key.code(), key.name(), key.year(), periodGrades.size(),
                    periodGrades.isEmpty() ? null : average(periodGrades), finalGrade);
        }).sorted(Comparator.comparingInt(StudentSubjectAverage::year)
                .thenComparing(StudentSubjectAverage::subjectCode).thenComparingInt(StudentSubjectAverage::subjectId)).toList();
    }

    public List<CourseSubjectAverage> courseSubjectAverages(List<ReportGrade> grades) {
        var grouped = grades.stream().collect(Collectors.groupingBy(grade -> new CourseKey(subjectKey(grade), grade.period())));
        return grouped.entrySet().stream().map(entry -> {
            var subject = entry.getKey().subject();
            return new CourseSubjectAverage(subject.id(), subject.code(), subject.name(), subject.year(), entry.getKey().period(),
                    (int) entry.getValue().stream().map(ReportGrade::studentId).distinct().count(), average(entry.getValue()));
        }).sorted(Comparator.comparingInt(CourseSubjectAverage::year).thenComparing(CourseSubjectAverage::subjectCode)
                .thenComparingInt(CourseSubjectAverage::subjectId).thenComparing(CourseSubjectAverage::period)).toList();
    }

    private SubjectKey subjectKey(ReportGrade grade) {
        return new SubjectKey(grade.subjectId(), grade.subjectCode(), grade.subjectName(), grade.year());
    }

    private BigDecimal average(List<ReportGrade> grades) {
        BigDecimal sum = grades.stream().map(ReportGrade::value).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(grades.size()), 1, RoundingMode.HALF_UP);
    }
}
