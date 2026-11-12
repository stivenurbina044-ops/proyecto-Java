package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AcademicReportDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.service.AcademicStatisticsService;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.ReportView;
import java.sql.SQLException;

public final class ReportController {
    private final ConsoleView console;
    private final ReportView view;
    private final AcademicReportDao reportDao;
    private final GradeDao gradeDao;
    private final AcademicStatisticsService statistics;

    public ReportController(ConsoleView console, ReportView view, AcademicReportDao reportDao,
                            GradeDao gradeDao, AcademicStatisticsService statistics) {
        this.console = console;
        this.view = view;
        this.reportDao = reportDao;
        this.gradeDao = gradeDao;
        this.statistics = statistics;
    }

    public void run() {
        while (true) {
            view.showMenu();
            int option = console.readMenuOption(0, 2).orElse(0);
            if (option == 0) {
                return;
            }
            try {
                if (option == 1) {
                    showStudentAverages();
                } else {
                    showCourseAverages();
                }
            } catch (IllegalArgumentException exception) {
                console.showError(exception.getMessage());
            } catch (SQLException exception) {
                ErrorLogger.log("ReportController: academic report failed", exception);
                console.showError("No se pudo consultar el reporte académico. " + exception.getMessage());
            }
        }
    }

    private void showStudentAverages() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var year = console.readAcademicYear();
        if (year.isPresent()) {
            var grades = reportDao.findByStudent(document.get(), year.getAsInt());
            view.showStudentAverages(document.get(), year.getAsInt(), statistics.studentSubjectAverages(grades));
        }
    }

    private void showCourseAverages() throws SQLException {
        var grades = gradeDao.findAll();
        console.showGrades(grades);
        if (grades.isEmpty()) {
            return;
        }
        var gradeId = console.readGradeId(grades);
        if (gradeId.isEmpty()) {
            return;
        }
        var year = console.readAcademicYear();
        if (year.isEmpty()) {
            return;
        }
        var period = console.readGradingPeriod();
        if (period.isPresent()) {
            var records = reportDao.findByGrade(gradeId.getAsInt(), year.getAsInt(), period.get());
            view.showCourseAverages(gradeId.getAsInt(), year.getAsInt(), period.get(), statistics.courseSubjectAverages(records));
        }
    }
}
