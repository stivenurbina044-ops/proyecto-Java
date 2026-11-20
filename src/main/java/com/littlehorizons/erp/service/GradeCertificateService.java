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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class GradeCertificateService {
    public static final int YEAR = 2025;

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
        StringBuilder text = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        for (List<Mark> marks : subjects.values()) {
            BigDecimal sum = marks.stream().map(Mark::value).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal average = sum.divide(BigDecimal.valueOf(marks.size()), 1, RoundingMode.HALF_UP);
            total = total.add(average);
            text.append(marks.get(0).subjectName()).append(": ").append(average).append('\n');
        }
        BigDecimal average = total.divide(BigDecimal.valueOf(subjects.size()), 1, RoundingMode.HALF_UP);
        return "CERTIFICADO DE NOTAS - AÑO " + YEAR + "\n" + student.name() + "\n" + text
                + "PROMEDIO GENERAL DEL AÑO: " + average + "\n" + issueDate + "\n";
    }
}
