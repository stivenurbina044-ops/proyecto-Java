package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoicePayment;
import com.littlehorizons.erp.model.SchoolBillingDetails;
import com.littlehorizons.erp.util.PdfDocument;
import io.nayuki.qrcodegen.QrCode;
import java.math.RoundingMode;
import java.util.Locale;

public class InvoicePdfWriter implements InvoiceDocumentWriter {
    @Override
    public byte[] render(Invoice invoice, InvoicePayment payment, SchoolBillingDetails school) {
        PdfDocument pdf = new PdfDocument();
        pdf.line("LITTLE HORIZONS", 22, true);
        pdf.line("FACTURA - SIMULACIÓN ACADÉMICA", 13, true);
        pdf.line("Datos ficticios. Sin validación ni autorización DIAN.", 9, false);
        pdf.rule();
        pdf.line(school.name(), 11, true);
        pdf.line("NIT de ejemplo: " + school.taxId(), 10, false);
        pdf.line(school.address(), 10, false);
        pdf.line("Teléfono: " + school.phone() + " | Correo: " + school.email(), 9, false);
        pdf.gap(8);
        pdf.line("Factura: " + invoice.number() + " | Emisión: " + invoice.issueDate(), 11, true);
        pdf.line("Pago ID: " + payment.paymentId() + " | Recibo: " + optional(payment.receiptNumber()), 9, false);
        pdf.line("Fecha de pago: " + payment.paymentDate() + " | Método: " + payment.paymentMethod(), 9, false);
        pdf.rule();
        pdf.line("CLIENTE / ACUDIENTE PAGADOR", 11, true);
        pdf.line(payment.guardianName(), 10, false);
        pdf.line(payment.guardianDocumentType() + ": " + payment.guardianDocument(), 10, false);
        pdf.line("Dirección: " + optional(payment.guardianAddress()), 10, false);
        pdf.line("Estudiante: " + payment.studentName() + " | Documento: " + payment.studentDocument(), 9, false);
        pdf.line("Año académico: " + optional(payment.academicYear()) + " | Período: " + optional(payment.period()), 9, false);
        pdf.rule();
        pdf.line("DETALLE DEL PAGO", 11, true);
        pdf.line("Descripción: " + payment.conceptName(), 10, false);
        String amount = payment.amount().setScale(2, RoundingMode.UNNECESSARY).toPlainString();
        pdf.line("Cantidad    Valor unitario (COP)       Subtotal (COP)", 10, true);
        pdf.line(String.format(Locale.ROOT, "%-12s%-26s%s", "1", amount, amount), 10, false);
        pdf.gap(8);
        pdf.line("Subtotal: " + amount + " COP", 10, false);
        pdf.line("IVA simulado: 0.00 COP", 10, false);
        pdf.line("TOTAL: " + amount + " COP", 13, true);
        pdf.line("El detalle representa el monto recibido; no certifica saldo de obligaciones.", 8, false);
        pdf.rule();
        pdf.line("ELEMENTOS SIMULADOS", 10, true);
        pdf.line("CUFE simulado (SHA-384): " + invoice.cufe(), 8, false);
        pdf.line(school.resolution(), 8, false);
        QrCode qr = QrCode.encodeText(qrPayload(invoice, payment, school), QrCode.Ecc.MEDIUM);
        float side = 144;
        pdf.ensureSpace(side + 40);
        pdf.line("QR de la factura simulada (no enlaza a la DIAN)", 8, false);
        float top = pdf.cursor();
        float module = side / (qr.size + 8);
        for (int y = 0; y < qr.size; y++) {
            for (int x = 0; x < qr.size; x++) {
                if (qr.getModule(x, y)) {
                    pdf.blackSquare(48 + (x + 4) * module, top - (y + 4) * module, module);
                }
            }
        }
        pdf.gap(side + 6);
        pdf.line("Ejercicio académico. CUFE, resolución y datos del colegio son simulados.", 8, false);
        return pdf.toBytes();
    }

    public String qrPayload(Invoice invoice, InvoicePayment payment, SchoolBillingDetails school) {
        return "SIMULACION ACADEMICA|FACTURA=" + invoice.number() + "|NIT=" + school.taxId()
                + "|FECHA=" + invoice.issueDate() + "|TOTAL=" + payment.amount().setScale(2).toPlainString()
                + "|CUFE=" + invoice.cufe();
    }

    private String optional(Object value) {
        return value == null || value.toString().isBlank() ? "Sin registrar" : value.toString();
    }
}
