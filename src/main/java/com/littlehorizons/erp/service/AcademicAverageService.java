package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.AcademicRecord;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class AcademicAverageService {
    public Map<Integer, Map<String, BigDecimal>> averagesByYearAndPeriod(List<AcademicRecord> records) {
        Map<Integer, Map<String, List<AcademicRecord>>> grouped = records.stream()
                .collect(Collectors.groupingBy(AcademicRecord::year, TreeMap::new,
                        Collectors.groupingBy(AcademicRecord::period, TreeMap::new, Collectors.toList())));
        Map<Integer, Map<String, BigDecimal>> averages = new TreeMap<>();
        grouped.forEach((year, periods) -> {
            Map<String, BigDecimal> periodAverages = new TreeMap<>();
            periods.forEach((period, grades) -> {
                BigDecimal total = grades.stream().map(AcademicRecord::grade)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                periodAverages.put(period, total.divide(BigDecimal.valueOf(grades.size()),
                        1, RoundingMode.HALF_UP));
            });
            averages.put(year, periodAverages);
        });
        return averages;
    }
}
