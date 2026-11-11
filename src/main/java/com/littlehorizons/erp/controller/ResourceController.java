package com.littlehorizons.erp.controller;

import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.ResourceLoanDao;
import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.util.ErrorLogger;
import com.littlehorizons.erp.view.ConsoleView;
import com.littlehorizons.erp.view.ResourceView;
import java.sql.SQLException;

public final class ResourceController {
    private final ConsoleView console;
    private final ResourceView view;
    private final ResourceDao resourceDao;
    private final ResourceLoanDao loanDao;

    public ResourceController(ConsoleView console, ResourceView view, ResourceDao resourceDao, ResourceLoanDao loanDao) {
        this.console = console;
        this.view = view;
        this.resourceDao = resourceDao;
        this.loanDao = loanDao;
    }

    public void run() {
        while (true) {
            view.showMenu();
            int option = console.readMenuOption(0, 7).orElse(0);
            if (option == 0) {
                return;
            }
            try {
                switch (option) {
                    case 1 -> view.showResources(resourceDao.findAll());
                    case 2 -> register();
                    case 3 -> lend(BorrowerType.STUDENT);
                    case 4 -> lend(BorrowerType.TEACHER);
                    case 5 -> view.showLoans(loanDao.findAll(false));
                    case 6 -> returnResource();
                    case 7 -> view.showLoans(loanDao.findAll(true));
                    default -> { }
                }
            } catch (IllegalArgumentException exception) {
                console.showError(exception.getMessage());
            } catch (SQLException exception) {
                ErrorLogger.log("ResourceController: database operation failed", exception);
                console.showError(exception.getErrorCode() == 1062 && option == 2
                        ? "Ya existe un recurso con ese código."
                        : "No se pudo completar la operación de recursos. " + exception.getMessage());
            }
        }
    }

    private void register() throws SQLException {
        var categories = resourceDao.findCategories();
        view.showCategories(categories);
        if (categories.isEmpty()) {
            console.showError("No hay categorías registradas para crear recursos.");
            return;
        }
        while (true) {
            view.showSelectionPrompt("categoría");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (categories.stream().noneMatch(category -> category.id() == id.getAsInt())) {
                console.showError("Selecciona una categoría del listado.");
                continue;
            }
            var form = view.readRegistration(id.getAsInt());
            if (form.isPresent()) {
                view.showSuccess("Recurso registrado", resourceDao.insert(form.get()));
            }
            return;
        }
    }

    private void lend(BorrowerType type) throws SQLException {
        var resources = resourceDao.findAll().stream()
                .filter(resource -> "Disponible".equals(resource.status())).toList();
        view.showResources(resources);
        if (resources.isEmpty()) {
            return;
        }
        while (true) {
            view.showSelectionPrompt("recurso");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (resources.stream().noneMatch(resource -> resource.id() == id.getAsInt())) {
                console.showError("Selecciona un recurso del listado.");
                continue;
            }
            var document = view.readBorrowerDocument(type);
            if (document.isEmpty()) {
                return;
            }
            var expectedReturn = view.readExpectedReturnDate();
            if (expectedReturn.isEmpty()) {
                return;
            }
            var notes = view.readNotes();
            if (notes.isPresent()) {
                view.showSuccess("Préstamo a " + type.label().toLowerCase() + " registrado",
                        loanDao.lend(id.getAsInt(), type, document.get(), expectedReturn.get(), notes.get()));
            }
            return;
        }
    }

    private void returnResource() throws SQLException {
        view.showReturnTypePrompt();
        int selected = console.readMenuOption(0, 2).orElse(0);
        if (selected == 0) {
            return;
        }
        BorrowerType type = selected == 1 ? BorrowerType.STUDENT : BorrowerType.TEACHER;
        var loans = loanDao.findAll(false).stream()
                .filter(loan -> loan.borrowerType() == type && loan.actualReturnDate() == null
                        && ("Activo".equals(loan.status()) || "Vencido".equals(loan.status()))).toList();
        view.showLoans(loans);
        if (loans.isEmpty()) {
            return;
        }
        while (true) {
            view.showSelectionPrompt("préstamo a devolver");
            var id = console.readMenuOption(0, Integer.MAX_VALUE);
            if (id.isEmpty() || id.getAsInt() == 0) {
                return;
            }
            if (loans.stream().noneMatch(loan -> loan.id() == id.getAsInt())) {
                console.showError("Selecciona un préstamo del listado.");
                continue;
            }
            loanDao.returnResource(type, id.getAsInt());
            view.showSuccess("Devolución registrada; recurso disponible", id.getAsInt());
            return;
        }
    }
}
