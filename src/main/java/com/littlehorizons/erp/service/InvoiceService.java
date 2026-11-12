package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.InvoiceDao;
import com.littlehorizons.erp.model.Invoice;
import com.littlehorizons.erp.model.SchoolBillingDetails;
import com.littlehorizons.erp.util.DatabaseConnection;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

public final class InvoiceService {
    private final InvoiceDao invoiceDao;
    private final InvoicePdfWriter writer;

    public InvoiceService(InvoiceDao invoiceDao, InvoicePdfWriter writer) {
        this.invoiceDao = invoiceDao;
        this.writer = writer;
    }

    public Invoice generate(int paymentId) throws SQLException, IOException {
        if (paymentId <= 0) {
            throw new IllegalArgumentException("Selecciona un ID de pago válido.");
        }
        SchoolBillingDetails school = SchoolBillingDetails.load();
        Path createdFile = null;
        try (var connection = DatabaseConnection.open()) {
            connection.setAutoCommit(false);
            try {
                var payment = invoiceDao.lockPayment(connection, paymentId);
                var existing = invoiceDao.findByPayment(connection, paymentId);
                if (existing.isPresent() && isReady(existing.get(), school.outputDirectory())) {
                    connection.commit();
                    return existing.get();
                }
                int id = existing.isPresent() ? existing.get().id() : invoiceDao.reserve(connection, payment);
                String number = existing.isPresent() ? existing.get().number()
                        : String.format(Locale.ROOT, "LH-%06d", id);
                LocalDate date = existing.isPresent() ? existing.get().issueDate() : payment.currentDate();
                String cufe = simulatedCufe(number, school.taxId(), date);
                if (existing.isPresent() && existing.get().cufe() != null && !cufe.equals(existing.get().cufe())) {
                    throw new IOException("La configuración del NIT no coincide con el CUFE guardado. Restaura la configuración original.");
                }
                String filename = String.format(Locale.ROOT, "factura_%06d_%s.pdf", id, UUID.randomUUID());
                Path destination = school.outputDirectory().resolve(filename);
                if (destination.toString().length() > 500) {
                    throw new IOException("La ruta del PDF supera los 500 caracteres admitidos en la base de datos.");
                }
                Invoice invoice = new Invoice(id, paymentId, number, date, cufe, filename, destination.toString());
                byte[] pdf = writer.render(invoice, payment, school);
                Files.createDirectories(school.outputDirectory());
                createdFile = Files.createTempFile(school.outputDirectory(), ".invoice-", ".tmp");
                Files.write(createdFile, pdf);
                Files.move(createdFile, destination);
                createdFile = destination;
                invoiceDao.complete(connection, invoice);
                connection.commit();
                return invoice;
            } catch (SQLException | IOException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                if (createdFile != null) {
                    try {
                        Files.deleteIfExists(createdFile);
                    } catch (IOException cleanupException) {
                        exception.addSuppressed(cleanupException);
                    }
                }
                throw exception;
            }
        }
    }

    private boolean isReady(Invoice invoice, Path outputDirectory) throws IOException {
        if (invoice.filePath() == null || invoice.fileName() == null || invoice.cufe() == null) {
            return false;
        }
        Path path = Path.of(invoice.filePath()).toAbsolutePath().normalize();
        if (!path.startsWith(outputDirectory) || !invoice.fileName().equals(path.getFileName().toString())) {
            throw new IOException("La ruta de la factura no pertenece a la carpeta configurada.");
        }
        if (!Files.isRegularFile(path)) {
            return false;
        }
        try (var stream = Files.newInputStream(path)) {
            return new String(stream.readNBytes(5), StandardCharsets.US_ASCII).equals("%PDF-");
        }
    }

    public static String simulatedCufe(String number, String taxId, LocalDate date) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-384")
                    .digest((number + "|" + taxId + "|" + date).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-384 is not available.", exception);
        }
    }
}
