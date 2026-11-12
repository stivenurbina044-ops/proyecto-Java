package com.littlehorizons.erp.service;

import com.littlehorizons.erp.model.Payment;
import java.math.BigDecimal;
import java.util.List;

public final class PaymentSummaryService {
    public BigDecimal total(List<Payment> payments) {
        return payments.stream().map(Payment::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
