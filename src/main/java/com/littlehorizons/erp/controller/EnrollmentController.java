package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EnrollmentDao;
import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.view.ConsoleView;
import java.sql.SQLException;

public final class EnrollmentController {
    private final ConsoleView view;
    private final EnrollmentDao enrollmentDao;
    private final GradeDao gradeDao;

    public EnrollmentController(ConsoleView view, EnrollmentDao enrollmentDao, GradeDao gradeDao) {
        this.view = view;
        this.enrollmentDao = enrollmentDao;
        this.gradeDao = gradeDao;
    }

    public void register() {
        try {
            var grades = gradeDao.findAll();
            view.showGrades(grades);
            if (grades.isEmpty()) {
                return;
            }
            var document = view.readStudentDocument();
            if (document.isEmpty()) {
                return;
            }
            var gradeId = view.readGradeId(grades);
            if (gradeId.isEmpty()) {
                return;
            }
            var year = view.readAcademicYear();
            if (year.isEmpty()) {
                return;
            }
            int id = enrollmentDao.enroll(document.get(), gradeId.getAsInt(), year.getAsInt());
            view.showEnrollmentRegistered(id);
        } catch (IllegalArgumentException exception) {
            view.showError(exception.getMessage());
        } catch (SQLException exception) {
            String message = switch (exception.getErrorCode()) {
                case 1062 -> "El estudiante ya tiene una matrícula para ese año académico.";
                case 1452 -> "El grado seleccionado ya no existe. Vuelve a consultar el catálogo.";
                default -> "No se pudo registrar la matrícula. " + exception.getMessage();
            };
            view.showError(message);
        }
    }

    public void showAll() {
        try {
            view.showEnrollments(enrollmentDao.findAll());
        } catch (SQLException exception) {
            view.showError("No se pudieron consultar las matrículas. " + exception.getMessage());
        }
    }
}
