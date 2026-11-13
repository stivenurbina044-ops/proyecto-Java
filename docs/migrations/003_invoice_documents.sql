-- Apply once to an existing installation. Back up the database first.
USE erp_little_horizons;
CREATE TABLE documentos_factura (
    id_factura INT PRIMARY KEY,
    contenido_pdf LONGBLOB NOT NULL,
    fecha_archivo TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_factura) REFERENCES facturas(id_factura) ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='Copia inmutable del PDF emitido para recuperación';
