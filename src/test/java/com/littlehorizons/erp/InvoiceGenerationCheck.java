package com.littlehorizons.erp;

import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.SchoolBillingDetails;
import com.littlehorizons.erp.model.InvoicePayment;
import com.littlehorizons.erp.service.InvoicePdfWriter;
import com.littlehorizons.erp.service.InvoiceDocumentWriter;
import com.littlehorizons.erp.service.InvoiceService;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;

/** Standalone checks with a JDBC double: never connects to the user's database. */
public final class InvoiceGenerationCheck {
    private static final class MemoryInvoiceDao extends InvoiceDao {
        private Invoice invoice;
        private int reservations;
        private boolean failComplete;

        @Override
        public InvoicePayment lockPayment(Connection connection, int paymentId) {
            return new InvoicePayment(paymentId, "Lucía Torres Gómez", "1234567890", "Mariana López Torres",
                    "CC", "12345678", "Calle 45 # 12-34, Bogotá", "Pensión mensual", new BigDecimal("350000.50"),
                    LocalDate.of(2026, 11, 11), "Transferencia", "PRUEBA-001", 2026, "III", LocalDate.of(2026, 11, 11));
        }

        @Override
        public Optional<Invoice> findByPayment(Connection connection, int paymentId) {
            return Optional.ofNullable(invoice);
        }

        @Override
        public int reserve(Connection connection, InvoicePayment payment) {
            reservations++;
            return 123;
        }

        @Override
        public void complete(Connection connection, Invoice value) throws SQLException {
            if (failComplete) {
                throw new SQLException("Injected metadata failure");
            }
            invoice = value;
        }
    }

    private static final class CountingWriter implements InvoiceDocumentWriter {
        private final InvoicePdfWriter delegate = new InvoicePdfWriter();
        private int renders;
        private boolean failRender;

        @Override
        public byte[] render(Invoice invoice, InvoicePayment payment,
                             com.littlehorizons.erp.model.SchoolBillingDetails school) {
            renders++;
            if (failRender) {
                throw new IllegalStateException("Injected renderer failure");
            }
            return delegate.render(invoice, payment, school);
        }
    }

    public static void main(String[] args) throws Exception {
        Path folder = Path.of(args[0]);
        MemoryInvoiceDao dao = new MemoryInvoiceDao();
        int[] rollbacks = {0};
        DriverManager.registerDriver(new Driver() {
            @Override
            public Connection connect(String url, Properties properties) {
                if (!acceptsURL(url)) {
                    return null;
                }
                Invoice before = dao.invoice;
                return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                        new Class<?>[] {Connection.class}, (proxy, method, values) -> {
                            if (method.getName().equals("rollback")) {
                                dao.invoice = before;
                                rollbacks[0]++;
                            }
                            if (method.getReturnType() == boolean.class) {
                                return false;
                            }
                            if (method.getReturnType() == int.class) {
                                return 0;
                            }
                            return null;
                        });
            }
            @Override public boolean acceptsURL(String url) { return "jdbc:invoice-check".equals(url); }
            @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties p) { return new DriverPropertyInfo[0]; }
            @Override public int getMajorVersion() { return 1; }
            @Override public int getMinorVersion() { return 0; }
            @Override public boolean jdbcCompliant() { return false; }
            @Override public Logger getParentLogger() { return Logger.getGlobal(); }
        });
        CountingWriter writer = new CountingWriter();
        InvoiceService service = new InvoiceService(dao, writer);
        Invoice first = service.generate(42);
        require(Files.isRegularFile(Path.of(first.filePath())), "PDF was not created");
        require(first.cufe().length() == 96, "CUFE must fit the database column");
        require(first.cufe().equals(InvoiceService.simulatedCufe(first.number(), SchoolBillingDetails.load().taxId(), first.issueDate())), "CUFE differs");
        Invoice second = service.generate(42);
        require(second.equals(first) && dao.reservations == 1 && writer.renders == 1, "Retry duplicated an invoice");
        Files.delete(Path.of(first.filePath()));
        Invoice regenerated = service.generate(42);
        require(regenerated.id() == first.id() && regenerated.number().equals(first.number())
                && regenerated.cufe().equals(first.cufe()) && dao.reservations == 1, "Regeneration changed invoice identity");
        byte[] pdf = Files.readAllBytes(Path.of(regenerated.filePath()));
        require(new String(pdf, 0, 8, StandardCharsets.US_ASCII).equals("%PDF-1.4"), "Invalid PDF header");
        Files.write(folder.resolve("preview.pdf"), pdf);
        Files.delete(Path.of(regenerated.filePath()));
        dao.invoice = null;
        writer.failRender = true;
        try {
            service.generate(42);
            throw new AssertionError("Renderer failure was not propagated");
        } catch (IllegalStateException expected) {
            require(dao.invoice == null, "Failed renderer persisted an invoice");
        }
        writer.failRender = false;
        dao.failComplete = true;
        try {
            service.generate(42);
            throw new AssertionError("Metadata failure was not propagated");
        } catch (SQLException expected) {
            require(dao.invoice == null, "Failed transaction persisted an invoice");
        }
        try (var files = Files.list(folder.resolve("invoices"))) {
            require(files.findAny().isEmpty(), "Failed generation left a partial PDF");
        }
        dao.failComplete = false;
        Invoice recovered = service.generate(42);
        require(Files.isRegularFile(Path.of(recovered.filePath())) && rollbacks[0] >= 2, "Recovery failed");
        try {
            service.generate(0);
            throw new AssertionError("Invalid payment ID accepted");
        } catch (IllegalArgumentException expected) {
            // Expected before JDBC access.
        }
        System.out.println("Invoice generation checks passed: PDF, CUFE, idempotency, missing-file regeneration, rollback and recovery.");
        System.out.println("JDBC double used; MySQL integration must be checked separately.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
