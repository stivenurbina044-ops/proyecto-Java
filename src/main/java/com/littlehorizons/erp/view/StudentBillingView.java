package com.littlehorizons.erp.view;

import com.littlehorizons.erp.model.ChargeRegistration;
import com.littlehorizons.erp.model.PaymentRegistration;
import com.littlehorizons.erp.model.StudentCharge;
import com.littlehorizons.erp.model.StudentRate;
import com.littlehorizons.erp.model.StudentRateRegistration;
import com.littlehorizons.erp.util.MoneyValidation;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

public final class StudentBillingView {
    private final Scanner input;
    private final PrintStream output;

    public StudentBillingView(Scanner input, PrintStream output) {
        this.input = input;
        this.output = output;
    }

    public Optional<StudentRateRegistration> readRate() {
        output.println("Tarifa personalizada por año. Escribe /volver para cancelar.");
        try {
            int year = readYear();
            BigDecimal amount;
            while (true) {
                String value = readText("Valor personalizado en COP (0 para beca completa, sin separadores de miles)", 30);
                if (value.matches("[0-9]{1,8}(?:[.,][0-9]{1,2})?")) {
                    amount = new BigDecimal(value.replace(',', '.'));
                    MoneyValidation.validate(amount, true);
                    break;
                }
                output.println("Usa un valor de 0 a 99999999,99, máximo dos decimales.");
            }
            String reason = readText("Motivo del ajuste", 1000);
            return Optional.of(new StudentRateRegistration(year, amount, reason));
        } catch (OperationCancelled exception) {
            output.println("Operación cancelada.");
            return Optional.empty();
        }
    }

    public Optional<ChargeRegistration> readCharge() {
        output.println("El cobro usará la tarifa del estudiante para ese año o, si no existe, el valor base.");
        output.println("Se permiten vencimientos pasados para registrar obligaciones históricas. /volver cancela.");
        try {
            int year = readYear();
            String period;
            output.println("Período: 1. I  2. II  3. III  4. IV  5. Anual");
            while (true) {
                String value = readText("Selecciona el período (1-5)", 1);
                if (value.matches("[1-5]")) {
                    period = PaymentRegistration.PERIODS.get(Integer.parseInt(value) - 1);
                    break;
                }
                output.println("Selecciona una opción entre 1 y 5.");
            }
            String reference = readText("Referencia única de la cuota (ej. PENSION-NOVIEMBRE)", 50);
            LocalDate dueDate;
            while (true) {
                String value = readText("Vencimiento (AAAA-MM-DD)", 10);
                try {
                    dueDate = LocalDate.parse(value);
                    if (dueDate.getYear() >= 1000 && dueDate.getYear() <= 9999) {
                        break;
                    }
                } catch (DateTimeParseException exception) {
                    // Retry invalid calendar dates.
                }
                output.println("Ingresa una fecha válida con año de cuatro cifras.");
            }
            return Optional.of(new ChargeRegistration(year, period, reference, dueDate));
        } catch (OperationCancelled exception) {
            output.println("Cobro cancelado.");
            return Optional.empty();
        }
    }

    public boolean confirm(String prompt) {
        try {
            while (true) {
                String value = readText(prompt + " (S/N, /volver para cancelar)", 10);
                if (value.equalsIgnoreCase("S")) {
                    return true;
                }
                if (value.equalsIgnoreCase("N")) {
                    return false;
                }
                output.println("Ingresa S o N.");
            }
        } catch (OperationCancelled exception) {
            return false;
        }
    }

    private int readYear() {
        while (true) {
            String value = readText("Año académico (cuatro cifras)", 4);
            if (value.matches("[1-9][0-9]{3}")) {
                return Integer.parseInt(value);
            }
            output.println("Ingresa un año entre 1000 y 9999.");
        }
    }

    private String readText(String prompt, int maximum) {
        while (true) {
            output.print(prompt + ": ");
            if (!input.hasNextLine()) {
                throw new OperationCancelled();
            }
            String value = input.nextLine().strip();
            if (value.equalsIgnoreCase("/volver")) {
                throw new OperationCancelled();
            }
            if (!value.isEmpty() && value.length() <= maximum) {
                return value;
            }
            output.printf("Ingresa de 1 a %d caracteres.%n", maximum);
        }
    }

    public void showRates(List<StudentRate> rates) {
        output.println("Tarifas personalizadas: " + rates.size());
        if (rates.isEmpty()) {
            output.println("No hay tarifas personalizadas. Los nuevos cobros usarán el valor base del concepto.");
        }
        for (StudentRate rate : rates) {
            output.printf("%s | Año: %d | Valor: %s COP | Motivo: %s%n",
                    rate.conceptName(), rate.year(), rate.amount().setScale(2).toPlainString(), rate.reason());
        }
    }

    public void showCharges(List<StudentCharge> charges) {
        output.println("Cobros encontrados: " + charges.size());
        output.println("Solo los pagos aplicados a un cobro reducen su saldo. Los pagos sin aplicar no se descuentan.");
        if (charges.isEmpty()) {
            output.println("No hay cobros que cumplan esta consulta.");
        }
        for (StudentCharge charge : charges) {
            output.printf("%d | %s - %s | %s | %s | Año: %d | Período: %s%n", charge.id(),
                    charge.studentDocument(), charge.studentName(), charge.conceptName(), charge.reference(), charge.year(), charge.period());
            output.printf("Cobrado: %s | Aplicado: %s | Saldo: %s COP | Vence: %s | Estado: %s%n",
                    charge.amount().setScale(2).toPlainString(), charge.paid().setScale(2).toPlainString(),
                    charge.balance().setScale(2).toPlainString(), charge.dueDate(), charge.status().label());
        }
    }

    public void showOverdueHeading() {
        output.println("Cobros con saldo pendiente y vencimiento anterior a hace un mes calendario (fecha de MySQL).");
    }

    private static final class OperationCancelled extends RuntimeException {
    }
}
