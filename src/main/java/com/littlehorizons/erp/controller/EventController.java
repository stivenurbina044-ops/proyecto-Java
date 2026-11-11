package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.EventDao;
import com.littlehorizons.erp.dao.TeacherDao;
import com.littlehorizons.erp.model.SchoolEvent;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.EventView;
import com.littlehorizons.erp.view.TeacherView;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class EventController {
    private final ConsoleView console;
    private final EventView view;
    private final TeacherView teacherView;
    private final TeacherDao teacherDao;
    private final EventDao eventDao;

    public EventController(ConsoleView console, EventView view, TeacherView teacherView,
                           TeacherDao teacherDao, EventDao eventDao) {
        this.console = console;
        this.view = view;
        this.teacherView = teacherView;
        this.teacherDao = teacherDao;
        this.eventDao = eventDao;
    }

    public void run() {
        while (true) {
            view.showMenu();
            int option = console.readMenuOption(0, 5).orElse(0);
            if (option == 0) {
                return;
            }
            try {
                switch (option) {
                    case 1 -> view.showEvents(eventDao.findAll());
                    case 2 -> register();
                    case 3 -> registerParticipant();
                    case 4 -> showParticipants();
                    case 5 -> recordPosition();
                    default -> { }
                }
            } catch (IllegalArgumentException exception) {
                console.showError(exception.getMessage());
            } catch (SQLException exception) {
                ErrorLogger.log("EventController: database operation failed", exception);
                console.showError(exception.getErrorCode() == 1062 && option == 3
                        ? "El estudiante ya está registrado en ese evento."
                        : "No se pudo completar la operación de eventos. " + exception.getMessage());
            }
        }
    }

    private void register() throws SQLException {
        var teachers = teacherDao.findAll().stream().filter(teacher -> "Activo".equals(teacher.status())).toList();
        teacherView.showTeachers(teachers);
        if (teachers.isEmpty()) {
            console.showError("Necesitas un profesor activo como organizador del evento.");
            return;
        }
        while (true) {
            view.showSelectionPrompt("profesor organizador");
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
                view.showSuccess("Evento registrado", eventDao.insert(form.get()));
            }
            return;
        }
    }

    private Optional<SchoolEvent> selectEvent(List<SchoolEvent> events) {
        view.showEvents(events);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        while (true) {
            view.showSelectionPrompt("evento");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return Optional.empty();
            }
            var selected = events.stream().filter(event -> event.id() == id.getAsInt()).findFirst();
            if (selected.isPresent()) {
                return selected;
            }
            console.showError("Selecciona un evento del listado.");
        }
    }

    private void registerParticipant() throws SQLException {
        var event = selectEvent(eventDao.findAll().stream().filter(SchoolEvent::active).toList());
        if (event.isEmpty()) {
            return;
        }
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var role = view.readRole(event.get().competition());
        if (role.isEmpty()) {
            return;
        }
        var notes = view.readNotes();
        if (notes.isPresent()) {
            view.showSuccess("Participante registrado", eventDao.registerParticipant(event.get().id(),
                    document.get(), role.get(), notes.get()));
        }
    }

    private void showParticipants() throws SQLException {
        var event = selectEvent(eventDao.findAll());
        if (event.isPresent()) {
            view.showParticipants(eventDao.findParticipants(event.get().id()));
        }
    }

    private void recordPosition() throws SQLException {
        var event = selectEvent(eventDao.findAll().stream()
                .filter(candidate -> candidate.active() && candidate.competition()
                        && !candidate.date().isAfter(LocalDate.now())).toList());
        if (event.isEmpty()) {
            return;
        }
        var participants = eventDao.findParticipants(event.get().id()).stream()
                .filter(participant -> "Competidor".equals(participant.role())).toList();
        view.showParticipants(participants);
        if (participants.isEmpty()) {
            return;
        }
        while (true) {
            view.showSelectionPrompt("participación del competidor");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (participants.stream().noneMatch(participant -> participant.id() == id.getAsInt())) {
                console.showError("Selecciona una participación del listado.");
                continue;
            }
            var position = view.readPosition();
            if (position.isPresent()) {
                eventDao.recordPosition(event.get().id(), id.getAsInt(), position.get());
                view.showSuccess("Resultado guardado", id.getAsInt());
            }
            return;
        }
    }
}
