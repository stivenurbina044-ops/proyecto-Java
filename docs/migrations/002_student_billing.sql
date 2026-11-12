USE erp_little_horizons;

-- Ampliación aditiva: conserva los pagos y facturas actuales.
CREATE TABLE tarifas_estudiante (
    id_estudiante INT NOT NULL,
    id_concepto INT NOT NULL,
    año_academico INT NOT NULL,
    monto_personalizado DECIMAL(10,2) NOT NULL,
    motivo VARCHAR(1000) NOT NULL,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id_estudiante, id_concepto, año_academico),
    CONSTRAINT chk_tarifa_monto CHECK (monto_personalizado >= 0),
    CONSTRAINT chk_tarifa_año CHECK (año_academico BETWEEN 1000 AND 9999),
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante),
    FOREIGN KEY (id_concepto) REFERENCES conceptos_pago(id_concepto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE cobros_estudiante (
    id_cobro INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_concepto INT NOT NULL,
    año_academico INT NOT NULL,
    periodo_academico ENUM('I', 'II', 'III', 'IV', 'Anual') NOT NULL,
    referencia VARCHAR(50) NOT NULL COMMENT 'Identifica la cuota, ej. PENSION-NOVIEMBRE',
    monto_cobrado DECIMAL(10,2) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY cobro_unico (id_estudiante, id_concepto, año_academico, referencia),
    INDEX idx_vencimiento (fecha_vencimiento),
    CONSTRAINT chk_cobro_monto CHECK (monto_cobrado >= 0),
    CONSTRAINT chk_cobro_año CHECK (año_academico BETWEEN 1000 AND 9999),
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante),
    FOREIGN KEY (id_concepto) REFERENCES conceptos_pago(id_concepto)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE aplicaciones_pago (
    id_pago INT NOT NULL PRIMARY KEY COMMENT 'Un pago se aplica completo a un solo cobro',
    id_cobro INT NOT NULL,
    fecha_aplicacion DATE NOT NULL,
    INDEX idx_cobro (id_cobro),
    FOREIGN KEY (id_pago) REFERENCES pagos(id_pago),
    FOREIGN KEY (id_cobro) REFERENCES cobros_estudiante(id_cobro)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
