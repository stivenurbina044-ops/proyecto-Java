package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.SubjectRegistrationDao;
import com.littlehorizons.erp.dao.AcademicRecordDao;
import com.littlehorizons.erp.service.AcademicAverageService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.util.ErrorLogger;
import java.sql.SQLException;

public final class AcademicController {
    private final ConsoleView view;
    private final SubjectRegistrationDao registrationDao;

    private final AcademicRecordDao recordDao;
    private final AcademicAverageService averageService;
    private final AttendanceController attendance;

    public AcademicController(ConsoleView view, SubjectRegistrationDao registrationDao,
                              AcademicRecordDao recordDao, AcademicAverageService averageService,
                              AttendanceController attendance) {
        this.view = view;
        this.registrationDao = registrationDao;
        this.recordDao = recordDao;
        this.averageService = averageService;
        this.attendance = attendance;
    }

    public void run() {
        while (true) {
            view.showAcademicMenu();
            int option = view.readMenuOption(0, 7).orElse(0);
            if (option == 0) {
                return;
            }
            try {
                if (option == 7) {
                    attendance.showWeeklyReport();
                } else if (option == 5) {
                    attendance.register();
                } else if (option == 6) {
                    attendance.showHistory();
                } else if (option == 4) {
                    registerGrade();
                } else if (option == 1) {
                    register();
                } else if (option == 3) {
                    showAcademicHistory();
                } else {
                    var document = view.readStudentDocument();
                    if (document.isPresent()) {
                        view.showSubjectRegistrations(registrationDao.findByStudent(document.get()));
                    }
                }
            } catch (IllegalArgumentException exception) {
                view.showError(exception.getMessage());
            } catch (SQLException exception) {
                ErrorLogger.log("AcademicController: database operation failed", exception);
                view.showError(exception.getErrorCode() == 1062
                        ? (option == 5 ? "Ya existe una asistencia para ese estudiante, materia y fecha."
                                : option == 4 ? "Ya existe una calificación para esa materia, año y período."
                                : "El estudiante ya tiene una inscripción en esa materia, año y período.")
                        : "No se pudo completar la operación académica. " + exception.getMessage());
            }
        }
    }

    private void registerGrade() throws SQLException {
        var document = view.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var year = view.readAcademicYear();
        if (year.isEmpty()) {
            return;
        }
        var period = view.readGradingPeriod();
        if (period.isEmpty()) {
            return;
        }
        var options = recordDao.findGradingOptions(document.get(), year.getAsInt(), period.get());
        if (options.isEmpty()) {
            view.showError("No hay materias calificables. Revisa la inscripción, el año, el período y la asignación de un profesor activo, cuya contratación ya haya iniciado, a la misma sección.");
            return;
        }
        var option = view.readGradingOption(options);
        if (option.isEmpty()) {
            return;
        }
        var grade = view.readGrade();
        if (grade.isEmpty()) {
            return;
        }
        view.showGradeRegistered(recordDao.insertGrade(document.get(), year.getAsInt(),
                period.get(), option.get(), grade.get()));
    }

    private void showAcademicHistory() throws SQLException {
        var document = view.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var records = recordDao.findByStudent(document.get());
        view.showAcademicRecords(records);
        view.showAcademicAverages(averageService.averagesByYearAndPeriod(records));
    }

    private void register() throws SQLException {
        var document = view.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var year = view.readAcademicYear();
        if (year.isEmpty()) {
            return;
        }
        var subjects = registrationDao.findAvailable(document.get(), year.getAsInt());
        if (subjects.isEmpty()) {
            view.showError("No hay materias disponibles. Revisa el documento, la matrícula activa y el catálogo del grado.");
            return;
        }
        var subjectId = view.readSubjectId(subjects);
        if (subjectId.isEmpty()) {
            return;
        }
        var period = view.readAcademicPeriod();
        if (period.isEmpty()) {
            return;
        }
        var section = view.readSection();
        if (section.isEmpty()) {
            return;
        }
        int id = registrationDao.register(document.get(), year.getAsInt(), period.get(),
                subjectId.getAsInt(), section.get());
        view.showSubjectRegistered(id);
    }
}
