package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.GradeCertificateDao;
import com.littlehorizons.erp.service.GradeCertificateService;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.GradeCertificateView;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;

public final class GradeCertificateController {
    private final GradeCertificateDao dao;
    private final GradeCertificateService service;
    private final GradeCertificateView view;

    public GradeCertificateController(GradeCertificateDao dao, GradeCertificateService service, GradeCertificateView view) {
        this.dao = dao;
        this.service = service;
        this.view = view;
    }

    public void run() {
        var selection = view.readStudentId();
        if (selection.isEmpty()) {
            return;
        }
        int id = selection.getAsInt();
        int year = GradeCertificateService.YEAR;
        try {
            var result = dao.find(id, year);
            if (result.isEmpty()) {
                view.showError("No se encontró ningún estudiante con el ID " + id + ". Verifique el ID e intente nuevamente.");
            } else if (result.get().marks().isEmpty()) {
                view.showError("El estudiante " + result.get().name() + " no tiene registros académicos para el año " + year + ".");
            } else {
                var student = result.get();
                view.showProgress(year);
                Path file = service.write(Path.of(""), id, student, LocalDate.now());
                view.showSuccess(student.name(), file, year);
            }
        } catch (SQLException exception) {
            ErrorLogger.log("GradeCertificateController: academic query failed", exception);
            view.showError("No se pudo consultar la información académica del estudiante.");
        } catch (IOException | SecurityException exception) {
            ErrorLogger.log("GradeCertificateController: certificate write failed", exception);
            view.showFileError();
        }
        view.waitForEnter();
    }
}
