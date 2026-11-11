package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.BorrowerType;
import com.littlehorizons.erp.model.ResourceCategory;
import com.littlehorizons.erp.model.ResourceLoan;
import com.littlehorizons.erp.model.ResourceRegistration;
import com.littlehorizons.erp.model.SchoolResource;
import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public final class ResourceView {
    private final Scanner input;
    private final PrintStream output;

    public ResourceView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void showMenu() {
        output.println("""
                Gestión de Recursos
                1. Listar recursos
                2. Registrar recurso
                3. Prestar a estudiante
                4. Prestar a profesor
                5. Consultar préstamos
                6. Registrar devolución
                7. Consultar préstamos vencidos
                0. Volver al menú principal
                """);
    }

    public void showCategories(List<ResourceCategory> categories) {
        output.println("Categorías de recursos:");
        for (ResourceCategory category : categories) {
            output.printf("%d | %s%n", category.id(), category.name());
        }
    }

    public void showSelectionPrompt(String entity) {
        output.println("Selecciona el ID de " + entity + " del listado. Usa 0 para volver.");
    }

    public Optional<ResourceRegistration> readRegistration(int categoryId) {
        output.println("Registro de recurso. Escribe /volver para cancelar.");
        try {
            String code = readText("Código único", false, 50);
            String name = readText("Nombre del recurso", false, 150);
            String description = readText("Descripción (opcional)", true, 1000);
            LocalDate acquisitionDate = readDate("Fecha de adquisición (AAAA-MM-DD, Enter para omitir)", true, false);
            String location = readText("Ubicación física", false, 100);
            return Optional.of(new ResourceRegistration(code, name, categoryId, description, acquisitionDate, location));
        } catch (OperationCancelled exception) {
            output.println("Registro cancelado.");
            return Optional.empty();
        }
    }

    public Optional<LocalDate> readExpectedReturnDate() {
        try {
            return Optional.of(readDate("Fecha de devolución esperada (AAAA-MM-DD)", false, true));
        } catch (OperationCancelled exception) {
            return Optional.empty();
        }
    }

    public Optional<String> readNotes() {
        try {
            return Optional.of(readText("Observaciones (opcional, /volver para cancelar)", true, 1000));
        } catch (OperationCancelled exception) {
            return Optional.empty();
        }
    }

    public Optional<String> readBorrowerDocument(BorrowerType type) {
        try {
            return Optional.of(readText("Documento de " + type.label().toLowerCase() + " (/volver para cancelar)", false, 20));
        } catch (OperationCancelled exception) {
            return Optional.empty();
        }
    }

    private LocalDate readDate(String prompt, boolean optional, boolean futureAllowed) {
        while (true) {
            String value = readText(prompt + " (/volver para cancelar)", optional, 10);
            if (optional && value.isEmpty()) {
                return null;
            }
            try {
                LocalDate date = LocalDate.parse(value);
                if (date.getYear() >= 1000 && date.getYear() <= 9999
                        && (futureAllowed ? !date.isBefore(LocalDate.now()) : !date.isAfter(LocalDate.now()))) {
                    return date;
                }
            } catch (DateTimeParseException exception) {
                // Retry invalid dates.
            }
            output.println(futureAllowed ? "Ingresa una fecha válida de hoy o posterior."
                    : "Ingresa una fecha válida que no sea futura.");
        }
    }

    private String readText(String prompt, boolean optional, int maxLength) {
        while (true) {
            output.print(prompt + ": ");
            if (!input.hasNextLine()) {
                throw new OperationCancelled();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                throw new OperationCancelled();
            }
            if ((optional || !value.isEmpty()) && value.length() <= maxLength) {
                return value;
            }
            output.printf("Ingresa un valor %sde máximo %d caracteres.%n",
                    optional ? "" : "obligatorio ", maxLength);
        }
    }

    public void showResources(List<SchoolResource> resources) {
        output.println("Recursos encontrados: " + resources.size());
        if (resources.isEmpty()) {
            output.println("No hay recursos disponibles para esta consulta.");
        }
        for (SchoolResource resource : resources) {
            output.printf("%d | %s | %s | %s | Estado: %s | Ubicación: %s%n", resource.id(), resource.code(),
                    resource.name(), resource.category(), resource.status(), displayOptional(resource.location()));
            output.printf("Descripción: %s | Adquisición: %s%n", displayOptional(resource.description()),
                    resource.acquisitionDate() == null ? "Sin registrar" : resource.acquisitionDate());
        }
    }

    public void showLoans(List<ResourceLoan> loans) {
        output.println("Préstamos encontrados: " + loans.size());
        if (loans.isEmpty()) {
            output.println("No hay préstamos para esta consulta.");
        }
        for (ResourceLoan loan : loans) {
            output.printf("%s - Préstamo %d | %s - %s | %s - %s | Estado: %s%n",
                    loan.borrowerType().label(), loan.id(), loan.resourceCode(), loan.resourceName(),
                    loan.borrowerDocument(), loan.borrowerName(), loan.status());
            output.printf("Préstamo: %s | Devolución esperada: %s | Devolución real: %s | Observaciones: %s%n",
                    loan.loanDate(), loan.expectedReturnDate(),
                    loan.actualReturnDate() == null ? "Pendiente" : loan.actualReturnDate(), displayOptional(loan.notes()));
        }
    }

    public void showReturnTypePrompt() {
        output.println("Tipo de préstamo: 1. Estudiante  2. Profesor  0. Volver");
    }

    public void showSuccess(String operation, int id) {
        output.printf("%s correctamente. ID: %d%n", operation, id);
    }

    private String displayOptional(String value) {
        return value == null || value.isBlank() ? "Sin registrar" : value;
    }

    private static final class OperationCancelled extends RuntimeException {
    }
}
