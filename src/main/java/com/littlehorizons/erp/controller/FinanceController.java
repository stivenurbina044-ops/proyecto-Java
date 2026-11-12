package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentGuardian;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.FinanceView;
import java.sql.SQLException;
import com.littlehorizons.erp.service.PaymentSummaryService;
import java.util.List;
import java.util.Optional;

public final class FinanceController {
    private final ConsoleView console;
    private final FinanceView view;
    private final PaymentConceptDao conceptDao;
    private final PaymentDao paymentDao;
    private final PaymentSummaryService summary;

    public FinanceController(ConsoleView console, FinanceView view, PaymentConceptDao conceptDao, PaymentDao paymentDao, PaymentSummaryService summary) {
        this.console = console;
        this.view = view;
        this.conceptDao = conceptDao;
        this.paymentDao = paymentDao;
        this.summary = summary;
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
                    case 1 -> view.showConcepts(conceptDao.findAll());
                    case 2 -> registerConcept();
                    case 3 -> registerPayment();
                    case 4 -> showStudentHistory();
                    case 5 -> showGuardianHistory();
                    default -> { }
                }
            } catch (IllegalArgumentException exception) {
                console.showError(exception.getMessage());
            } catch (SQLException exception) {
                ErrorLogger.log("FinanceController: database operation failed", exception);
                console.showError(exception.getErrorCode() == 1062
                        ? (option == 2 ? "Ya existe un concepto con ese código." : "Ya existe un pago con ese número de recibo.")
                        : "No se pudo completar la operación financiera. " + exception.getMessage());
            }
        }
    }

    private void registerConcept() throws SQLException {
        var form = view.readConceptRegistration();
        if (form.isPresent()) {
            view.showSuccess("Concepto registrado", conceptDao.insert(form.get()));
        }
    }

    private void registerPayment() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var guardian = selectGuardian(paymentDao.findGuardians(document.get()));
        if (guardian.isEmpty()) {
            return;
        }
        var concept = selectConcept(conceptDao.findAll().stream().filter(PaymentConcept::active).toList());
        if (concept.isEmpty()) {
            return;
        }
        var form = view.readPaymentRegistration(concept.get());
        if (form.isPresent() && view.confirmPayment(document.get(), guardian.get(), concept.get(), form.get())) {
            int paymentId = paymentDao.insert(document.get(), guardian.get().id(), concept.get().id(), form.get());
            view.showSuccess("Pago registrado", paymentId);
        }
    }

    private Optional<PaymentGuardian> selectGuardian(List<PaymentGuardian> guardians) {
        view.showGuardians(guardians);
        if (guardians.isEmpty()) {
            console.showError("El estudiante no existe o no tiene acudientes vinculados.");
            return Optional.empty();
        }
        while (true) {
            view.showSelectionPrompt("acudiente que realiza el pago");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return Optional.empty();
            }
            var selected = guardians.stream().filter(guardian -> guardian.id() == id.getAsInt()).findFirst();
            if (selected.isPresent()) {
                return selected;
            }
            console.showError("Selecciona un acudiente del listado.");
        }
    }

    private Optional<PaymentConcept> selectConcept(List<PaymentConcept> concepts) {
        view.showConcepts(concepts);
        if (concepts.isEmpty()) {
            return Optional.empty();
        }
        while (true) {
            view.showSelectionPrompt("concepto de pago");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return Optional.empty();
            }
            var selected = concepts.stream().filter(concept -> concept.id() == id.getAsInt()).findFirst();
            if (selected.isPresent()) {
                return selected;
            }
            console.showError("Selecciona un concepto del listado.");
        }
    }

    private void showStudentHistory() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isPresent()) {
            var payments = paymentDao.findByStudent(document.get());
            view.showPayments(payments, summary.total(payments));
        }
    }

    private void showGuardianHistory() throws SQLException {
        var document = view.readGuardianDocument();
        if (document.isPresent()) {
            var payments = paymentDao.findByGuardian(document.get());
            view.showPayments(payments, summary.total(payments));
        }
    }
}
