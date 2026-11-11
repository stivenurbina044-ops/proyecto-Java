package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.dao.ClassScheduleDao;
import com.littlehorizons.erp.dao.TeacherAssignmentDao;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.TeacherView;
import java.sql.SQLException;

public final class TeacherController {
    private final ConsoleView console;
    private final TeacherView view;
    private final TeacherDao teacherDao;
    private final TeacherAssignmentDao assignmentDao;
    private final ClassScheduleDao scheduleDao;

    public TeacherController(ConsoleView console, TeacherView view, TeacherDao teacherDao, TeacherAssignmentDao assignmentDao, ClassScheduleDao scheduleDao) {
        this.console = console;
        this.view = view;
        this.teacherDao = teacherDao;
        this.assignmentDao = assignmentDao;
        this.scheduleDao = scheduleDao;
    }

    public void run() {
        while (true) {
            view.showMenu();
            int option = console.readMenuOption(0, 6).orElse(0);
            if (option == 0) {
                return;
            }
            try {
                if (option == 1) {
                    view.showTeachers(teacherDao.findAll());
                } else if (option == 5) {
                    registerSchedule();
                } else if (option == 6) {
                    view.showSchedules(scheduleDao.findAll());
                } else if (option == 4) {
                    view.showAssignments(assignmentDao.findAll());
                } else if (option == 3) {
                    assignSubject();
                } else {
                    var form = view.readRegistration();
                    if (form.isPresent()) {
                        int id = teacherDao.insert(form.get());
                        view.showRegistered(id);
                    }
                }
            } catch (IllegalArgumentException exception) {
                console.showError(exception.getMessage());
            } catch (SQLException exception) {
                ErrorLogger.log("TeacherController: database operation failed", exception);
                console.showError(exception.getErrorCode() == 1062
                        ? (option == 5 ? "El horario ya está registrado." : option == 3 ? "La asignación ya está registrada."
                                : "El documento o el correo electrónico ya está registrado.")
                        : "No se pudo completar la operación de profesores. " + exception.getMessage());
            }
        }
    }
    private void registerSchedule() throws SQLException {
        var assignments = assignmentDao.findAll();
        view.showAssignments(assignments);
        if (assignments.isEmpty()) {
            return;
        }
        while (true) {
            view.showScheduleAssignmentPrompt();
            var selected = console.readMenuOption(0, Integer.MAX_VALUE);
            if (selected.isEmpty() || selected.getAsInt() == 0) {
                return;
            }
            int id = selected.getAsInt();
            if (assignments.stream().noneMatch(assignment -> assignment.id() == id)) {
                console.showError("Selecciona una asignación del listado.");
                continue;
            }
            var form = view.readSchedule();
            if (form.isPresent()) {
                view.showScheduleRegistered(scheduleDao.register(id, form.get()));
            }
            return;
        }
    }

    private void assignSubject() throws SQLException {
        var teachers = teacherDao.findAll().stream()
                .filter(teacher -> "Activo".equals(teacher.status())).toList();
        view.showTeachers(teachers);
        if (teachers.isEmpty()) {
            return;
        }
        int teacherId;
        while (true) {
            view.showAssignmentPrompt();
            var selected = console.readMenuOption(0, Integer.MAX_VALUE);
            if (selected.isEmpty() || selected.getAsInt() == 0) {
                return;
            }
            teacherId = selected.getAsInt();
            int candidate = teacherId;
            if (teachers.stream().anyMatch(teacher -> teacher.id() == candidate)) {
                break;
            }
            console.showError("Selecciona un profesor del listado.");
        }
        var subjects = assignmentDao.findActiveSubjects();
        if (subjects.isEmpty()) {
            console.showError("No hay materias activas para asignar.");
            return;
        }
        var subjectId = console.readSubjectId(subjects);
        if (subjectId.isEmpty()) {
            return;
        }
        var year = console.readAcademicYear();
        if (year.isEmpty()) {
            return;
        }
        var section = console.readSection();
        if (section.isEmpty()) {
            return;
        }
        view.showAssignmentRegistered(assignmentDao.assign(teacherId, subjectId.getAsInt(),
                year.getAsInt(), section.get()));
    }
}
