package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GradeDao;
import com.littlehorizons.erp.dao.GuardianDao;
import com.littlehorizons.erp.dao.StudentDao;
import com.littlehorizons.erp.service.StudentRegistrationService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.StudentRegistrationView;
import com.littlehorizons.erp.util.ErrorLogger;
import java.sql.SQLException;

public final class MainMenuController {
    private static final int EXIT_OPTION = 8;
    private final ConsoleView view;
    private final StudentDao studentDao;
    private final GuardianDao guardianDao;
    private final GradeDao gradeDao;
    private final EnrollmentController enrollment;
    private final AcademicController academic;
    private final TeacherController teachers;
    private final ClubController clubs;

    private final StudentRegistrationView registrationView;
    private final StudentRegistrationService registration;

    public MainMenuController(ConsoleView view, StudentDao studentDao, GuardianDao guardianDao, GradeDao gradeDao,
                              StudentRegistrationView registrationView, StudentRegistrationService registration,
                              EnrollmentController enrollment, AcademicController academic, TeacherController teachers,
                              ClubController clubs) {
        this.view = view;
        this.studentDao = studentDao;
        this.guardianDao = guardianDao;
        this.gradeDao = gradeDao;
        this.enrollment = enrollment;
        this.academic = academic;
        this.teachers = teachers;
        this.clubs = clubs;
        this.registrationView = registrationView;
        this.registration = registration;
    }

    public void run() {
        while (true) {
            view.showMainMenu();
            int option = view.readMenuOption(1, EXIT_OPTION).orElse(EXIT_OPTION);
            if (option == EXIT_OPTION) {
                view.showGoodbye();
                return;
            }
            if (option == 1) {
                runStudentMenu();
            } else if (option == 2) {
                academic.run();
            } else if (option == 3) {
                teachers.run();
            } else if (option == 4) {
                clubs.run();

            } else {
                view.showModulePending();
            }
        }
    }

    private void runStudentMenu() {
        while (true) {
            view.showStudentMenu();
            int option = view.readMenuOption(0, 6).orElse(0);
            switch (option) {
                case 1 -> showStudents();
                case 2 -> registerStudent();
                case 3 -> registerGuardian();
                case 4 -> showGrades();
                case 5 -> enrollment.register();
                case 6 -> enrollment.showAll();
                default -> {
                    return;
                }
            }
        }
    }

    private void registerStudent() {
        var form = registrationView.readRegistration();
        if (form.isEmpty()) {
            return;
        }

        var student = form.get();
        try {
            int studentId = registration.registerWithExistingGuardian(
                    student.documentType(), student.documentNumber(), student.firstName(),
                    student.lastName(), student.birthDate(), student.gender(), student.admissionDate(),
                    student.guardianDocumentNumber());
            view.showStudentRegistered(studentId);
        } catch (IllegalArgumentException exception) {
            view.showError(exception.getMessage());
        } catch (SQLException exception) {
            ErrorLogger.log("MainMenuController: database operation failed", exception);
            if (exception.getErrorCode() == 1062) {
                view.showError("El documento del estudiante ya está registrado.");
            } else {
                view.showError("No se pudo registrar al estudiante. " + exception.getMessage());
            }
        }
    }

    private void registerGuardian() {
        var form = registrationView.readGuardianRegistration();
        if (form.isEmpty()) {
            return;
        }

        try {
            view.showGuardianRegistered(guardianDao.insertGuardian(form.get()));
        } catch (SQLException exception) {
            ErrorLogger.log("MainMenuController: database operation failed", exception);
            if (exception.getErrorCode() == 1062) {
                view.showError("El acudiente ya está registrado. Puedes usar su documento al registrar al estudiante.");
            } else {
                view.showError("No se pudo registrar al acudiente. " + exception.getMessage());
            }
        }
    }

    private void showGrades() {
        try {
            view.showGrades(gradeDao.findAll());
        } catch (SQLException exception) {
            ErrorLogger.log("MainMenuController: database operation failed", exception);
            view.showError("No se pudo consultar el catálogo de grados. " + exception.getMessage());
        }
    }

    private void showStudents() {
        try {
            view.showStudents(studentDao.findAll());
        } catch (SQLException exception) {
            ErrorLogger.log("MainMenuController: database operation failed", exception);
            view.showError("No se pudo consultar el listado de estudiantes. " + exception.getMessage());
        }
    }
}
