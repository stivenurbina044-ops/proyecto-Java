package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ClubDao;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ClubView;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.TeacherView;
import java.sql.SQLException;

public final class ClubController {
    private final ConsoleView console;
    private final ClubView view;
    private final TeacherView teacherView;
    private final ClubDao clubDao;
    private final EventController events;

    public ClubController(ConsoleView console, ClubView view, TeacherView teacherView, ClubDao clubDao, EventController events) {
        this.console = console;
        this.view = view;
        this.teacherView = teacherView;
        this.clubDao = clubDao;
        this.events = events;
    }

    public void run() {
        while (true) {
            view.showMenu();
            int option = console.readMenuOption(0, 6).orElse(0);
            if (option == 0) {
                return;
            }
            try {
                switch (option) {
                    case 1 -> view.showClubs(clubDao.findAll());
                    case 2 -> register();
                    case 3 -> enroll();
                    case 4 -> showMemberships();
                    case 5 -> withdraw();
                    case 6 -> events.run();
                    default -> { }
                }
            } catch (IllegalArgumentException exception) {
                console.showError(exception.getMessage());
            } catch (SQLException exception) {
                ErrorLogger.log("ClubController: database operation failed", exception);
                console.showError(exception.getErrorCode() == 1062 && option == 2
                        ? "Ya existe un club con ese nombre."
                        : "No se pudo completar la operación de clubes. " + exception.getMessage());
            }
        }
    }

    private void register() throws SQLException {
        var teachers = clubDao.findSupervisors();
        teacherView.showTeachers(teachers);
        if (teachers.isEmpty()) {
            console.showError("Necesitas un profesor activo cuya contratación ya haya iniciado.");
            return;
        }
        while (true) {
            view.showSelectionPrompt("profesor encargado");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (teachers.stream().noneMatch(teacher -> teacher.id() == id.getAsInt())) {
                console.showError("Selecciona un profesor del listado.");
                continue;
            }
            var form = view.readRegistration(id.getAsInt());
            if (form.isPresent()) {
                view.showRegistered(clubDao.insert(form.get()));
            }
            return;
        }
    }

    private void enroll() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var clubs = clubDao.findAll().stream().filter(club -> club.active()).toList();
        view.showClubs(clubs);
        if (clubs.isEmpty()) {
            return;
        }
        while (true) {
            view.showSelectionPrompt("club");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (clubs.stream().noneMatch(club -> club.id() == id.getAsInt())) {
                console.showError("Selecciona un club del listado.");
                continue;
            }
            view.showEnrolled(clubDao.enroll(document.get(), id.getAsInt()));
            return;
        }
    }

    private void withdraw() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var memberships = clubDao.findByStudent(document.get()).stream()
                .filter(membership -> "Activa".equals(membership.status())).toList();
        view.showMemberships(memberships);
        if (memberships.isEmpty()) {
            console.showError("El estudiante no tiene membresías activas para retirar.");
            return;
        }
        while (true) {
            view.showSelectionPrompt("membresía que deseas retirar");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (memberships.stream().noneMatch(membership -> membership.id() == id.getAsInt())) {
                console.showError("Selecciona una membresía del listado.");
                continue;
            }
            var reason = view.readWithdrawalReason();
            if (reason.isPresent()) {
                clubDao.withdraw(document.get(), id.getAsInt(), reason.get());
                view.showWithdrawn();
            }
            return;
        }
    }

    private void showMemberships() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isPresent()) {
            view.showMemberships(clubDao.findByStudent(document.get()));
        }
    }
}
