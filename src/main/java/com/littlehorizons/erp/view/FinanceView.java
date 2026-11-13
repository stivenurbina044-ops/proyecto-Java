package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.Payment;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.PaymentConcept;
import com.littlehorizons.erp.model.PaymentConceptRegistration;
import com.littlehorizons.erp.model.PaymentGuardian;
import com.littlehorizons.erp.model.PaymentRegistration;
import com.littlehorizons.erp.util.MoneyValidation;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public final class FinanceView {
    private final Scanner input;
    private final PrintStream output;

    public FinanceView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public void showMenu() {
        output.println("""
                Gestión Financiera
                1. Listar conceptos de pago
                2. Registrar concepto de pago
                3. Registrar pago
                4. Consultar pagos de estudiante
                5. Consultar pagos realizados por acudiente
                6. Generar o consultar factura PDF de pago
                7. Configurar tarifa personalizada
                8. Consultar tarifas de estudiante
                9. Crear cobro con vencimiento
                10. Consultar cobros y saldos de estudiante
                11. Aplicar pago existente a cobro
                12. Reporte de cobros pendientes por más de un mes
                0. Volver al menú principal
                """);
    }

    public void showConcepts(List<PaymentConcept> concepts) {
        output.println("Conceptos encontrados: " + concepts.size());
        if (concepts.isEmpty()) {
            output.println("No hay conceptos disponibles para esta consulta.");
        }
        for (PaymentConcept concept : concepts) {
            output.printf("%d | %s | %s | Base: %s COP | Recurrente: %s | %s%n", concept.id(), concept.code(),
                    concept.name(), money(concept.baseAmount()), concept.recurring() ? "Sí" : "No",
                    concept.active() ? "Activo" : "Inactivo");
            output.println("Descripción: " + displayOptional(concept.description()));
        }
    }

    public Optional<PaymentConceptRegistration> readConceptRegistration() {
        output.println("Registro de concepto. Escribe /volver para cancelar.");
        try {
            String code = readText("Código único", false, 20);
            String name = readText("Nombre", false, 100);
            String description = readText("Descripción (opcional)", true, 1000);
            BigDecimal baseAmount = readAmount("Valor base en COP", true);
            boolean recurring = readChoice("¿Es recurrente?", List.of("Sí", "No")).equals("Sí");
            return Optional.of(new PaymentConceptRegistration(code, name, description, baseAmount, recurring));
        } catch (OperationCancelled exception) {
            output.println("Registro cancelado.");
            return Optional.empty();
        }
    }

    public Optional<PaymentRegistration> readPaymentRegistration(PaymentConcept concept) {
        output.println("Registro de pago. Escribe /volver para cancelar.");
        output.printf("Concepto: %s | Valor base de referencia: %s COP%n", concept.name(), money(concept.baseAmount()));
        try {
            BigDecimal amount = readAmount("Monto realmente pagado en COP", false);
            LocalDate date = readDate();
            String method = readChoice("Método de pago", PaymentRegistration.METHODS);
            String receipt = readText("Número de recibo único", false, 50);
            int year;
            while (true) {
                String value = readText("Año académico al que aplica (cuatro cifras)", false, 4);
                if (value.matches("[1-9][0-9]{3}")) {
                    year = Integer.parseInt(value);
                    break;
                }
                output.println("Ingresa un año entre 1000 y 9999.");
            }
            String period = readChoice("Período al que aplica", PaymentRegistration.PERIODS);
            String notes = readText("Notas (opcional)", true, 1000);
            return Optional.of(new PaymentRegistration(amount, date, method, receipt, year, period, notes));
        } catch (OperationCancelled exception) {
            output.println("Pago cancelado.");
            return Optional.empty();
        }
    }

    public boolean confirmPayment(String document, PaymentGuardian guardian, PaymentConcept concept,
                                  PaymentRegistration payment) {
        output.printf("Estudiante: %s | Acudiente: %s (%s)%n", document, guardian.fullName(), guardian.documentNumber());
        output.printf("Concepto: %s | Monto: %s COP | Fecha: %s | Método: %s%n",
                concept.name(), money(payment.amount()), payment.date(), payment.method());
        output.printf("Recibo: %s | Año: %d | Período: %s | Notas: %s%n",
                payment.receiptNumber(), payment.academicYear(), payment.period(), displayOptional(payment.notes()));
        try {
            return readChoice("¿Guardar este pago?", List.of("Confirmar", "Cancelar")).equals("Confirmar");
        } catch (OperationCancelled exception) {
            return false;
        }
    }

    private BigDecimal readAmount(String prompt, boolean allowZero) {
        while (true) {
            String value = readText(prompt + " (sin separadores de miles, máximo dos decimales)", false, 30);
            if (value.matches("[0-9]{1,8}(?:[.,][0-9]{1,2})?")) {
                BigDecimal amount = new BigDecimal(value.replace(',', '.'));
                try {
                    MoneyValidation.validate(amount, allowZero);
                    return amount;
                } catch (IllegalArgumentException exception) {
                    output.println(exception.getMessage());
                }
            } else {
                output.println("Usa un importe como 350000 o 350000,50, máximo 99999999,99.");
            }
        }
    }

    private LocalDate readDate() {
        while (true) {
            String value = readText("Fecha de pago (AAAA-MM-DD)", false, 10);
            try {
                LocalDate date = LocalDate.parse(value);
                if (date.getYear() >= 1000 && !date.isAfter(LocalDate.now())) {
                    return date;
                }
            } catch (DateTimeParseException exception) {
                // Retry invalid calendar dates.
            }
            output.println("Ingresa una fecha válida que no sea futura.");
        }
    }

    private String readChoice(String prompt, List<String> choices) {
        output.println(prompt + " (0 o /volver para cancelar):");
        for (int index = 0; index < choices.size(); index++) {
            output.printf("%d. %s%n", index + 1, choices.get(index));
        }
        while (true) {
            String value = readText("Selecciona una opción", false, 10);
            if (value.equals("0")) {
                throw new OperationCancelled();
            }
            try {
                int selected = Integer.parseInt(value);
                if (selected >= 1 && selected <= choices.size()) {
                    return choices.get(selected - 1);
                }
            } catch (NumberFormatException exception) {
                // Retry invalid menu entries.
            }
            output.println("Selecciona una opción del listado.");
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

    public Optional<String> readGuardianDocument() {
        try {
            return Optional.of(readText("Documento del acudiente (/volver para cancelar)", false, 20));
        } catch (OperationCancelled exception) {
            return Optional.empty();
        }
    }

    public void showGuardians(List<PaymentGuardian> guardians) {
        output.println("Acudientes vinculados: " + guardians.size());
        for (PaymentGuardian guardian : guardians) {
            output.printf("%d | %s | %s | Responsable financiero: %s%n", guardian.id(), guardian.documentNumber(),
                    guardian.fullName(), guardian.financiallyResponsible() ? "Sí" : "No");
        }
    }

    public void showSelectionPrompt(String entity) {
        output.println("Selecciona el ID de " + entity + " del listado. Usa 0 para volver.");
    }

    public void showPaymentAllocationReminder() {
        output.println("Si este pago cubre un cobro registrado, aplícalo desde la opción 11 para reducir su saldo.");
    }

    public void showInvoice(Invoice invoice) {
        output.println("Factura: " + invoice.number());
        output.println("PDF guardado en: " + invoice.filePath());
    }

    public void showSuccess(String operation, int id) {
        output.printf("%s correctamente. ID: %d%n", operation, id);
    }

    public void showPayments(List<Payment> payments, BigDecimal total) {
        output.println("Pagos encontrados: " + payments.size());
        if (payments.isEmpty()) {
            output.println("No hay pagos para el documento consultado.");
        }
        for (Payment payment : payments) {
            output.printf("%d | %s | %s - %s | Acudiente: %s%n", payment.id(), payment.date(),
                    payment.studentDocument(), payment.studentName(), displayOptional(payment.guardianName()));
            output.printf("%s | %s COP | %s | Recibo: %s | Año: %s | Período: %s%n",
                    payment.conceptName(), money(payment.amount()), payment.method(), displayOptional(payment.receiptNumber()),
                    payment.academicYear() == null ? "Sin registrar" : payment.academicYear(), displayOptional(payment.period()));
            output.println("Notas: " + displayOptional(payment.notes()));
        }
        if (!payments.isEmpty()) {
            output.println("Total de pagos consultados: " + money(total) + " COP");
        }
    }

    private String money(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private String displayOptional(String value) {
        return value == null || value.isBlank() ? "Sin registrar" : value;
    }

    private static final class OperationCancelled extends RuntimeException {
    }
}
