package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoicePayment;
import com.littlehorizons.erp.model.SchoolBillingDetails;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.StringJoiner;

public class InvoiceTextWriter implements InvoiceDocumentWriter {
    static final String TITLE = "REPORTE DE PAGO";

    @Override
    public byte[] render(Invoice invoice, InvoicePayment payment, SchoolBillingDetails school) {
        StringJoiner text = new StringJoiner("\n", "", "\n");
        text.add(TITLE);
        text.add("LITTLE HORIZONS");
        text.add("FACTURA ELECTRÓNICA DE VENTA");
        text.add("");
        text.add(school.name());
        text.add("NIT: " + school.taxId());
        text.add(school.address());
        text.add("Teléfono: " + school.phone() + " | Correo: " + school.email());
        text.add("");
        text.add("Factura: " + invoice.number() + " | Emisión: " + invoice.issueDate());
        text.add("Pago ID: " + payment.paymentId() + " | Recibo: " + optional(payment.receiptNumber()));
        text.add("Fecha de pago: " + payment.paymentDate() + " | Método: " + payment.paymentMethod());
        text.add("");
        text.add("CLIENTE / ACUDIENTE PAGADOR");
        text.add(payment.guardianName());
        text.add(payment.guardianDocumentType() + ": " + payment.guardianDocument());
        text.add("Dirección: " + optional(payment.guardianAddress()));
        text.add("Estudiante: " + payment.studentName() + " | Documento: " + payment.studentDocument());
        text.add("Año académico: " + optional(payment.academicYear()) + " | Período: " + optional(payment.period()));
        text.add("");
        text.add("DETALLE DEL PAGO");
        text.add("Descripción: " + payment.conceptName());
        String amount = payment.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        text.add("Cantidad    Valor unitario (COP)       Subtotal (COP)");
        text.add(String.format(Locale.ROOT, "%-12s%-26s%s", "1", amount, amount));
        text.add("");
        text.add("Subtotal: " + amount + " COP");
        text.add("IVA: 0.00 COP");
        text.add("TOTAL: " + amount + " COP");
        text.add("El detalle representa el monto recibido; no certifica saldo de obligaciones.");
        text.add("");
        text.add("INFORMACIÓN DE FACTURACIÓN");
        text.add("CUFE: " + invoice.cufe());
        text.add(school.resolution());
        return text.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String optional(Object value) {
        return value == null || value.toString().isBlank() ? "Sin registrar" : value.toString();
    }
}
