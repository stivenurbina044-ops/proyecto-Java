package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.AttendanceDao;
import com.littlehorizons.erp.view.AttendanceView;
import com.littlehorizons.erp.view.ConsoleView;
import java.sql.SQLException;

public final class AttendanceController {
    private final ConsoleView console;
    private final AttendanceView view;
    private final AttendanceDao attendanceDao;

    public AttendanceController(ConsoleView console, AttendanceView view, AttendanceDao attendanceDao) {
        this.console = console;
        this.view = view;
        this.attendanceDao = attendanceDao;
    }

    public void register() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var date = view.readDate();
        if (date.isEmpty()) {
            return;
        }
        var period = console.readAcademicPeriod();
        if (period.isEmpty()) {
            return;
        }
        var options = attendanceDao.findOptions(document.get(), date.get(), period.get());
        if (options.isEmpty()) {
            console.showError("No hay materias habilitadas. Revisa matrícula, inscripción activa, sección y contratación del profesor para esa fecha.");
            return;
        }
        var option = console.readGradingOption(options);
        if (option.isEmpty()) {
            return;
        }
        var status = view.readStatus();
        if (status.isEmpty()) {
            return;
        }
        var notes = view.readNotes();
        if (notes.isEmpty()) {
            return;
        }
        view.showRegistered(attendanceDao.register(document.get(), date.get(), period.get(),
                option.get(), status.get(), notes.get()));
    }

    public void showWeeklyReport() throws SQLException {
        var date = view.readReportDate();
        if (date.isPresent()) {
            view.showWeeklyAbsences(date.get(), attendanceDao.findWeeklyAbsences(date.get()));
        }
    }

    public void showHistory() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isPresent()) {
            view.showHistory(attendanceDao.findByStudent(document.get()));
        }
    }
}
