package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.GradeCertificateDao.Mark;
import com.littlehorizons.erp.dao.GradeCertificateDao.StudentGrades;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public final class GradeCertificateService {
    public static final int YEAR = 2025;
    private static final String RULE = "=".repeat(66);
    private static final String TABLE_RULE = "-".repeat(77);
    private static final String ROW = "%-33s%-8s%-8s%-9s%-9s%s%n";
    private static final List<String> PERIODS = List.of("I", "II", "III", "IV");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/uuuu");

    public Path write(Path directory, int id, StudentGrades student, LocalDate issueDate) throws IOException {
        if (student.marks().isEmpty()) {
            throw new IllegalArgumentException("El certificado requiere al menos una nota.");
        }
        Path destination = directory.toAbsolutePath().normalize().resolve("certificado_" + id + "_" + YEAR + ".txt");
        Path temporary = Files.createTempFile(destination.getParent(), ".certificado-", ".tmp");
        try {
            Files.writeString(temporary, format(student, issueDate), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
        return destination;
    }

    private String format(StudentGrades student, LocalDate issueDate) {
        Map<String, List<Mark>> subjects = student.marks().stream()
                .collect(Collectors.groupingBy(Mark::subjectCode, LinkedHashMap::new, Collectors.toList()));
        StringBuilder text = new StringBuilder("""
                %s
                           COLEGIO INTERNACIONAL "LITTLE HORIZONS"
                                  CERTIFICADO DE NOTAS - AÑO %d
                %s

                DATOS DEL ESTUDIANTE
                %s
                Nombre     : %s
                Documento  : %s %s
                Grado      : %s

                %s
                                        REGISTRO DE CALIFICACIONES
                %s

                """.formatted(RULE, YEAR, RULE, TABLE_RULE, student.name(), student.documentType(),
                student.document(), student.grade() == null ? "Sin matrícula registrada" : student.grade(), RULE, RULE));
        text.append(String.format(Locale.ROOT, ROW, "Materia", "Per I", "Per II", "Per III", "Per IV", "Promedio"));
        text.append(TABLE_RULE).append(System.lineSeparator());

        BigDecimal total = BigDecimal.ZERO;
        for (List<Mark> marks : subjects.values()) {
            Map<String, BigDecimal> byPeriod = marks.stream().collect(Collectors.toMap(Mark::period, Mark::value));
            BigDecimal sum = marks.stream().map(Mark::value).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = sum.divide(BigDecimal.valueOf(marks.size()), 1, RoundingMode.HALF_UP);
            total = total.add(average);
            List<String> values = PERIODS.stream().map(period -> {
                BigDecimal value = byPeriod.get(period);
                return value == null ? "—" : value.setScale(1, RoundingMode.HALF_UP).toPlainString();
            }).toList();
            String name = marks.get(0).subjectName().replaceAll("\\s+", " ").strip();
            // Wrap long names within the subject column and keep the grades on its first line.
            text.append(String.format(Locale.ROOT, ROW, name.substring(0, Math.min(name.length(), 32)),
                    values.get(0), values.get(1), values.get(2), values.get(3), average.toPlainString()));
            for (int start = 32; start < name.length(); start += 32) {
                text.append(name.substring(start, Math.min(start + 32, name.length())).strip())
                        .append(System.lineSeparator());
            }
        }
        BigDecimal average = total.divide(BigDecimal.valueOf(subjects.size()), 1, RoundingMode.HALF_UP);
        text.append("""

                %s
                PROMEDIO GENERAL DEL AÑO: %s
                %s

                Fecha de emisión: %s


                ___________________________
                Secretaría Académica
                Colegio Internacional "Little Horizons"
                """.formatted(RULE, average.toPlainString(), RULE, issueDate.format(DATE_FORMAT)));
        return text.toString();
    }
}
