package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.PaymentConceptDao;
import com.littlehorizons.erp.dao.PaymentDao;
import com.littlehorizons.erp.dao.StudentBillingDao;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.service.PaymentSummaryService;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.FinanceView;
import com.littlehorizons.erp.view.StudentBillingView;
import java.sql.SQLException;
import java.util.Optional;

public final class StudentBillingController {
    private final ConsoleView console;
    private final FinanceView financeView;
    private final StudentBillingView view;
    private final PaymentConceptDao conceptDao;
    private final PaymentDao paymentDao;
    private final StudentBillingDao billingDao;
    private final PaymentSummaryService summary;

    public StudentBillingController(ConsoleView console, FinanceView financeView, StudentBillingView view,
            PaymentConceptDao conceptDao, PaymentDao paymentDao, StudentBillingDao billingDao, PaymentSummaryService summary) {
        this.console = console;
        this.financeView = financeView;
        this.view = view;
        this.conceptDao = conceptDao;
        this.paymentDao = paymentDao;
        this.billingDao = billingDao;
        this.summary = summary;
    }

    public void setRate() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var concept = selectConcept();
        if (concept.isEmpty()) {
            return;
        }
        var form = view.readRate();
        if (form.isPresent() && view.confirm("¿Guardar o reemplazar esta tarifa para el estudiante, concepto y año indicados?")) {
            var rate = form.get();
            billingDao.setRate(document.get(), concept.get().id(), rate.year(), rate.amount(), rate.reason());
            view.showRates(billingDao.findRates(document.get()));
        }
    }

    public void showRates() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isPresent()) {
            view.showRates(billingDao.findRates(document.get()));
        }
    }

    public void createCharge() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var concept = selectConcept();
        if (concept.isEmpty()) {
            return;
        }
        var form = view.readCharge();
        if (form.isPresent() && view.confirm("¿Crear el cobro con el valor vigente del concepto o la tarifa personalizada?")) {
            int id = billingDao.createCharge(document.get(), concept.get().id(), form.get());
            financeView.showSuccess("Cobro registrado", id);
            view.showCharges(billingDao.findCharges(document.get(), false));
        }
    }

    public void showCharges() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isPresent()) {
            view.showCharges(billingDao.findCharges(document.get(), false));
        }
    }

    public void showOverdue() throws SQLException {
        view.showOverdueHeading();
        view.showCharges(billingDao.findCharges(null, true));
    }

    public void applyPayment() throws SQLException {
        var document = console.readStudentDocument();
        if (document.isEmpty()) {
            return;
        }
        var payments = paymentDao.findByStudent(document.get());
        financeView.showPayments(payments, summary.total(payments));
        if (payments.isEmpty()) {
            return;
        }
        int paymentId;
        while (true) {
            financeView.showSelectionPrompt("pago que deseas aplicar completo");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (payments.stream().anyMatch(payment -> payment.id() == id.getAsInt())) {
                paymentId = id.getAsInt();
                break;
            }
            console.showError("Selecciona un pago del listado.");
        }
        var charges = billingDao.findCharges(document.get(), false).stream()
                .filter(charge -> charge.balance().signum() > 0).toList();
        view.showCharges(charges);
        if (charges.isEmpty()) {
            return;
        }
        while (true) {
            financeView.showSelectionPrompt("cobro al que aplicarás el pago");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (charges.stream().noneMatch(charge -> charge.id() == id.getAsInt())) {
                console.showError("Selecciona un cobro del listado.");
                continue;
            }
            if (view.confirm("¿Aplicar el pago " + paymentId + " al cobro " + id.getAsInt() + "?")) {
                billingDao.applyPayment(document.get(), paymentId, id.getAsInt());
                financeView.showSuccess("Pago aplicado sin crear otro pago ni otra factura", paymentId);
                view.showCharges(billingDao.findCharges(document.get(), false));
            }
            return;
        }
    }

    private Optional<PaymentConcept> selectConcept() throws SQLException {
        var concepts = conceptDao.findAll().stream().filter(PaymentConcept::active).toList();
        financeView.showConcepts(concepts);
        if (concepts.isEmpty()) {
            return Optional.empty();
        }
        while (true) {
            financeView.showSelectionPrompt("concepto");
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
}
