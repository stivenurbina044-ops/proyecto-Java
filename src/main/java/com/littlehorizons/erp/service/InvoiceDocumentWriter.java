package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.InvoicePayment;
import com.littlehorizons.erp.model.SchoolBillingDetails;

/** Renders invoice bytes without persisting files or database records. */
@FunctionalInterface
public interface InvoiceDocumentWriter {
    byte[] render(Invoice invoice, InvoicePayment payment, SchoolBillingDetails school);
}
