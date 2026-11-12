-- Little Horizons ERP - MySQL 8.0 o superior.
-- Instalación inicial: ejecutar únicamente sobre una base nueva y vacía.
-- Detener la ejecución ante el primer error. No ejecutar sobre la base de trabajo.
-- No elimina bases ni tablas existentes. Las actualizaciones van en migrations/.

-- ============================================
-- SECCIÓN 1 - DDL
-- ============================================
CREATE DATABASE erp_little_horizons CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE erp_little_horizons;

-- 1. ESTUDIANTES Y ACUDIENTES
CREATE TABLE grados (
    id_grado INT AUTO_INCREMENT PRIMARY KEY,
    nombre_grado VARCHAR(50) NOT NULL UNIQUE COMMENT 'Preescolar a Once',
    nivel_numerico INT NOT NULL UNIQUE COMMENT '0 para preescolar, 1-11 para grados',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Catálogo de grados académicos del colegio';

CREATE TABLE estudiantes (
    id_estudiante INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('TI', 'CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'TI',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    fecha_nacimiento DATE NOT NULL,
    genero ENUM('M', 'F', 'Otro') NOT NULL,
    direccion VARCHAR(255),
    telefono VARCHAR(20),
    correo_electronico VARCHAR(100),
    tipo_sangre VARCHAR(5),
    alergias TEXT,
    condiciones_medicas TEXT,
    fecha_ingreso DATE NOT NULL,
    estado ENUM('Activo', 'Graduado', 'Retirado', 'Suspendido') DEFAULT 'Activo',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_estado (estado),
    INDEX idx_nombre (primer_apellido, segundo_apellido, primer_nombre)
) ENGINE=InnoDB COMMENT='Información completa de estudiantes';

CREATE TABLE acudientes (
    id_acudiente INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    parentesco VARCHAR(50) NOT NULL COMMENT 'Padre, Madre, Abuelo, Tío, Tutor Legal, etc.',
    telefono_principal VARCHAR(20) NOT NULL,
    telefono_secundario VARCHAR(20),
    correo_electronico VARCHAR(100),
    direccion VARCHAR(255),
    ocupacion VARCHAR(100),
    es_responsable_financiero BOOLEAN DEFAULT TRUE COMMENT 'Indica si es responsable de pagos',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_nombre (primer_apellido, segundo_apellido)
) ENGINE=InnoDB COMMENT='Acudientes y tutores legales';

CREATE TABLE estudiante_acudiente (
    id_estudiante INT NOT NULL,
    id_acudiente INT NOT NULL,
    es_principal BOOLEAN DEFAULT FALSE COMMENT 'Acudiente principal para comunicaciones',
    es_contacto_emergencia BOOLEAN DEFAULT TRUE COMMENT 'Disponible para emergencias',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id_estudiante, id_acudiente),
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_acudiente) REFERENCES acudientes(id_acudiente) ON DELETE CASCADE,
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_acudiente (id_acudiente)
) ENGINE=InnoDB COMMENT='Relación entre estudiantes y sus acudientes';

CREATE TABLE matriculas (
    id_matricula INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_grado INT NOT NULL,
    año_academico INT NOT NULL COMMENT 'Año académico',
    estado_matricula ENUM('Activa', 'Completada', 'Retirada') DEFAULT 'Activa',
    fecha_matricula DATE NOT NULL,
    fecha_retiro DATE,
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_grado) REFERENCES grados(id_grado),
    UNIQUE KEY matricula_unica (id_estudiante, año_academico),
    INDEX idx_año_grado (año_academico, id_grado),
    INDEX idx_estado (estado_matricula)
) ENGINE=InnoDB COMMENT='Historial de matrículas de estudiantes por año y grado';

-- 2. MÓDULO ACADÉMICO
CREATE TABLE materias (
    id_materia INT AUTO_INCREMENT PRIMARY KEY,
    codigo_materia VARCHAR(20) NOT NULL UNIQUE COMMENT 'Código único de la materia',
    nombre_materia VARCHAR(100) NOT NULL,
    descripcion TEXT,
    id_grado INT NOT NULL COMMENT 'Grado al que pertenece la materia',
    horas_semanales INT DEFAULT 2 COMMENT 'Horas semanales asignadas',
    esta_activa BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_grado) REFERENCES grados(id_grado),
    INDEX idx_grado (id_grado),
    INDEX idx_activa (esta_activa),
    INDEX idx_codigo (codigo_materia)
) ENGINE=InnoDB COMMENT='Catálogo de materias por grado';

CREATE TABLE profesores (
    id_profesor INT AUTO_INCREMENT PRIMARY KEY,
    tipo_documento ENUM('CC', 'CE', 'Pasaporte') NOT NULL DEFAULT 'CC',
    numero_documento VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    primer_apellido VARCHAR(100) NOT NULL,
    segundo_apellido VARCHAR(100),
    correo_electronico VARCHAR(100) NOT NULL UNIQUE,
    telefono VARCHAR(20),
    especializacion VARCHAR(100) COMMENT 'Área de especialización',
    fecha_contratacion DATE NOT NULL,
    estado ENUM('Activo', 'Inactivo', 'Licencia') DEFAULT 'Activo',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_documento (tipo_documento, numero_documento),
    INDEX idx_estado (estado),
    INDEX idx_nombre (primer_apellido, segundo_apellido)
) ENGINE=InnoDB COMMENT='Información de profesores';

CREATE TABLE asignaciones_profesor (
    id_asignacion INT AUTO_INCREMENT PRIMARY KEY,
    id_profesor INT NOT NULL,
    id_materia INT NOT NULL,
    año_academico INT NOT NULL,
    seccion VARCHAR(10) COMMENT 'Sección o grupo (A, B, C)',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    UNIQUE KEY asignacion_unica (id_profesor, id_materia, año_academico, seccion),
    INDEX idx_profesor_año (id_profesor, año_academico),
    INDEX idx_materia_año (id_materia, año_academico)
) ENGINE=InnoDB COMMENT='Asignación de profesores a materias por año académico';

-- Ampliación respecto al anexo: horarios semanales por asignación.
-- La aplicación controla los cruces del profesor mediante una transacción.
CREATE TABLE horarios_clase (
    id_horario INT NOT NULL AUTO_INCREMENT,
    id_asignacion INT NOT NULL,
    dia_semana TINYINT NOT NULL COMMENT '1=Lunes, 7=Domingo',
    hora_inicio TIME NOT NULL,
    hora_fin TIME NOT NULL,
    PRIMARY KEY (id_horario),
    UNIQUE KEY horario_unico (id_asignacion, dia_semana, hora_inicio, hora_fin),
    CONSTRAINT chk_horario_dia CHECK (dia_semana BETWEEN 1 AND 7),
    CONSTRAINT chk_horario_horas CHECK (
        hora_inicio >= '00:00:00' AND hora_fin < '24:00:00' AND hora_inicio < hora_fin
    ),
    CONSTRAINT fk_horario_asignacion FOREIGN KEY (id_asignacion)
        REFERENCES asignaciones_profesor (id_asignacion) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inscripciones_materia (
    id_inscripcion INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    año_academico INT NOT NULL,
    periodo_academico ENUM('I', 'II', 'III', 'IV') NOT NULL COMMENT 'Período académico',
    seccion VARCHAR(10) COMMENT 'Sección o grupo asignado',
    fecha_inscripcion DATE NOT NULL,
    estado ENUM('Activa', 'Retirada', 'Completada') DEFAULT 'Activa',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    UNIQUE KEY inscripcion_unica (id_estudiante, id_materia, año_academico, periodo_academico),
    INDEX idx_estudiante_año (id_estudiante, año_academico),
    INDEX idx_materia_periodo (id_materia, periodo_academico)
) ENGINE=InnoDB COMMENT='Inscripción de estudiantes en materias específicas';

CREATE TABLE registros_academicos (
    id_registro INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    id_profesor INT NOT NULL,
    año_academico INT NOT NULL,
    periodo_academico ENUM('I', 'II', 'III', 'IV', 'Final') NOT NULL,
    valor_nota DECIMAL(2,1) NOT NULL COMMENT 'Nota de 0.0 a 5.0 según estándar colombiano',
    observaciones TEXT,
    fecha_registro DATE NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor),
    CHECK (valor_nota >= 0.0 AND valor_nota <= 5.0),
    UNIQUE KEY registro_unico (id_estudiante, id_materia, año_academico, periodo_academico),
    INDEX idx_estudiante_año (id_estudiante, año_academico),
    INDEX idx_materia_periodo (id_materia, periodo_academico),
    INDEX idx_valor_nota (valor_nota)
) ENGINE=InnoDB COMMENT='Registro de calificaciones de estudiantes';

CREATE TABLE asistencias (
    id_asistencia INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_materia INT NOT NULL,
    fecha_asistencia DATE NOT NULL,
    estado_asistencia ENUM('Presente', 'Ausente', 'Tardanza', 'Excusa Médica') NOT NULL,
    observaciones TEXT,
    registrado_por INT COMMENT 'ID del profesor que registró la asistencia',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_materia) REFERENCES materias(id_materia) ON DELETE CASCADE,
    FOREIGN KEY (registrado_por) REFERENCES profesores(id_profesor),
    UNIQUE KEY asistencia_unica (id_estudiante, id_materia, fecha_asistencia),
    INDEX idx_estudiante_fecha (id_estudiante, fecha_asistencia),
    INDEX idx_materia_fecha (id_materia, fecha_asistencia),
    INDEX idx_estado (estado_asistencia)
) ENGINE=InnoDB COMMENT='Registro de asistencia diaria de estudiantes';

-- 3. ACTIVIDADES EXTRACURRICULARES
CREATE TABLE clubes (
    id_club INT AUTO_INCREMENT PRIMARY KEY,
    nombre_club VARCHAR(100) NOT NULL UNIQUE,
    descripcion TEXT,
    id_profesor_supervisor INT COMMENT 'Profesor encargado del club',
    capacidad_maxima INT DEFAULT 30 COMMENT 'Cupo máximo de estudiantes',
    horario_reunion VARCHAR(100) COMMENT 'Días y horario de reunión',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_profesor_supervisor) REFERENCES profesores(id_profesor),
    INDEX idx_activo (esta_activo),
    INDEX idx_nombre (nombre_club)
) ENGINE=InnoDB COMMENT='Catálogo de clubes extracurriculares';

CREATE TABLE membresias_club (
    id_membresia INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_club INT NOT NULL,
    fecha_ingreso DATE NOT NULL,
    estado_membresia ENUM('Activa', 'Retirada') DEFAULT 'Activa',
    fecha_retiro DATE,
    motivo_retiro TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_club) REFERENCES clubes(id_club) ON DELETE CASCADE,
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_club_estado (id_club, estado_membresia)
) ENGINE=InnoDB COMMENT='Membresías de estudiantes en clubes';

CREATE TABLE eventos (
    id_evento INT AUTO_INCREMENT PRIMARY KEY,
    nombre_evento VARCHAR(150) NOT NULL,
    descripcion TEXT,
    tipo_evento ENUM('Académico', 'Cultural', 'Deportivo', 'Social', 'Científico') NOT NULL,
    fecha_evento DATE NOT NULL,
    lugar VARCHAR(150),
    tiene_competencia BOOLEAN DEFAULT FALSE COMMENT 'Indica si el evento tiene competencia con posiciones',
    id_organizador INT COMMENT 'Profesor organizador del evento',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_organizador) REFERENCES profesores(id_profesor),
    INDEX idx_fecha (fecha_evento),
    INDEX idx_tipo (tipo_evento),
    INDEX idx_activo (esta_activo)
) ENGINE=InnoDB COMMENT='Catálogo de eventos del colegio';

CREATE TABLE participantes_evento (
    id_participacion INT AUTO_INCREMENT PRIMARY KEY,
    id_evento INT NOT NULL,
    id_estudiante INT NOT NULL,
    rol_participacion ENUM('Participante', 'Competidor', 'Organizador', 'Asistente') DEFAULT 'Participante',
    posicion_obtenida VARCHAR(20) COMMENT 'Posición obtenida si hay competencia',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_evento) REFERENCES eventos(id_evento) ON DELETE CASCADE,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    UNIQUE KEY participacion_unica (id_evento, id_estudiante),
    INDEX idx_evento (id_evento),
    INDEX idx_estudiante (id_estudiante)
) ENGINE=InnoDB COMMENT='Registro de participación de estudiantes en eventos';

-- 4. MÓDULO FINANCIERO
CREATE TABLE conceptos_pago (
    id_concepto INT AUTO_INCREMENT PRIMARY KEY,
    codigo_concepto VARCHAR(20) NOT NULL UNIQUE,
    nombre_concepto VARCHAR(100) NOT NULL,
    descripcion TEXT,
    monto_base DECIMAL(10,2) NOT NULL COMMENT 'Valor base del concepto en pesos colombianos',
    es_recurrente BOOLEAN DEFAULT FALSE COMMENT 'Indica si es un pago recurrente',
    esta_activo BOOLEAN DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_activo (esta_activo),
    INDEX idx_codigo (codigo_concepto)
) ENGINE=InnoDB COMMENT='Catálogo de conceptos de pago del colegio';

CREATE TABLE pagos (
    id_pago INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_acudiente INT COMMENT 'Acudiente que realizó el pago',
    id_concepto INT NOT NULL,
    monto_pagado DECIMAL(10,2) NOT NULL,
    fecha_pago DATE NOT NULL,
    metodo_pago ENUM('Efectivo', 'Transferencia', 'Tarjeta Débito', 'Tarjeta Crédito', 'Cheque') NOT NULL,
    numero_recibo VARCHAR(50) UNIQUE,
    año_academico INT COMMENT 'Año al que aplica el pago',
    periodo_academico ENUM('I', 'II', 'III', 'IV', 'Anual') COMMENT 'Período al que aplica el pago',
    notas TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    FOREIGN KEY (id_acudiente) REFERENCES acudientes(id_acudiente),
    FOREIGN KEY (id_concepto) REFERENCES conceptos_pago(id_concepto),
    INDEX idx_estudiante_fecha (id_estudiante, fecha_pago),
    INDEX idx_concepto (id_concepto),
    INDEX idx_fecha (fecha_pago),
    INDEX idx_recibo (numero_recibo)
) ENGINE=InnoDB COMMENT='Registro de todos los pagos realizados';

CREATE TABLE facturas (
    id_factura INT AUTO_INCREMENT PRIMARY KEY,
    numero_factura VARCHAR(50) NOT NULL UNIQUE COMMENT 'Número consecutivo de factura',
    id_pago INT NOT NULL COMMENT 'Pago asociado a esta factura',
    fecha_emision DATE NOT NULL,
    cufe VARCHAR(100) COMMENT 'Código Único de Factura Electrónica (simulado)',
    nombre_archivo_pdf VARCHAR(255) COMMENT 'Nombre del archivo PDF generado',
    ruta_archivo VARCHAR(500) COMMENT 'Ruta completa donde se guardó el PDF',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_pago) REFERENCES pagos(id_pago) ON DELETE CASCADE,
    INDEX idx_numero_factura (numero_factura),
    INDEX idx_fecha_emision (fecha_emision),
    INDEX idx_pago (id_pago)
) ENGINE=InnoDB COMMENT='Registro de facturas electrónicas generadas';

-- 5. GESTIÓN DE RECURSOS
CREATE TABLE categorias_recurso (
    id_categoria INT AUTO_INCREMENT PRIMARY KEY,
    nombre_categoria VARCHAR(50) NOT NULL UNIQUE,
    descripcion TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Categorías de recursos';

CREATE TABLE recursos (
    id_recurso INT AUTO_INCREMENT PRIMARY KEY,
    codigo_recurso VARCHAR(50) NOT NULL UNIQUE COMMENT 'Código de inventario único',
    nombre_recurso VARCHAR(150) NOT NULL,
    id_categoria INT NOT NULL,
    descripcion TEXT,
    fecha_adquisicion DATE,
    ubicacion_fisica VARCHAR(100) COMMENT 'Ubicación física',
    estado_recurso ENUM('Disponible', 'Prestado', 'En Mantenimiento', 'Dado de Baja') DEFAULT 'Disponible',
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_categoria) REFERENCES categorias_recurso(id_categoria),
    INDEX idx_codigo (codigo_recurso),
    INDEX idx_estado (estado_recurso),
    INDEX idx_categoria (id_categoria),
    INDEX idx_nombre (nombre_recurso)
) ENGINE=InnoDB COMMENT='Inventario de recursos prestables del colegio';

CREATE TABLE prestamos_estudiante (
    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
    id_recurso INT NOT NULL,
    id_estudiante INT NOT NULL COMMENT 'Estudiante que toma prestado el recurso',
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion_esperada DATE NOT NULL,
    fecha_devolucion_real DATE,
    estado_prestamo ENUM('Activo', 'Devuelto', 'Vencido') DEFAULT 'Activo',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_recurso) REFERENCES recursos(id_recurso) ON DELETE CASCADE,
    FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante) ON DELETE CASCADE,
    INDEX idx_recurso_estado (id_recurso, estado_prestamo),
    INDEX idx_estudiante (id_estudiante),
    INDEX idx_estado (estado_prestamo),
    INDEX idx_fecha_devolucion (fecha_devolucion_esperada)
) ENGINE=InnoDB COMMENT='Registro de préstamos de recursos a estudiantes';

CREATE TABLE prestamos_profesor (
    id_prestamo INT AUTO_INCREMENT PRIMARY KEY,
    id_recurso INT NOT NULL,
    id_profesor INT NOT NULL COMMENT 'Profesor que toma prestado el recurso',
    fecha_prestamo DATE NOT NULL,
    fecha_devolucion_esperada DATE NOT NULL,
    fecha_devolucion_real DATE,
    estado_prestamo ENUM('Activo', 'Devuelto', 'Vencido') DEFAULT 'Activo',
    observaciones TEXT,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (id_recurso) REFERENCES recursos(id_recurso) ON DELETE CASCADE,
    FOREIGN KEY (id_profesor) REFERENCES profesores(id_profesor) ON DELETE CASCADE,
    INDEX idx_recurso_estado (id_recurso, estado_prestamo),
    INDEX idx_profesor (id_profesor),
    INDEX idx_estado (estado_prestamo),
    INDEX idx_fecha_devolucion (fecha_devolucion_esperada)
) ENGINE=InnoDB COMMENT='Registro de préstamos de recursos a profesores';

-- Ampliación de tarifas, cobros y aplicación de pagos.
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

-- ============================================
-- SECCIÓN 2 - DML: CATÁLOGOS Y 30 REGISTROS POR TABLA PRINCIPAL
-- ============================================
-- Incluye el lote ficticio LHSEED. No repetir seed_data.sql después de instalar.
-- Detener al primer error; el DDL previo no se revierte con ROLLBACK.
START TRANSACTION;

INSERT INTO grados (nombre_grado, nivel_numerico) VALUES
('Preescolar', 0), ('Primero', 1), ('Segundo', 2), ('Tercero', 3),
('Cuarto', 4), ('Quinto', 5), ('Sexto', 6), ('Séptimo', 7),
('Octavo', 8), ('Noveno', 9), ('Décimo', 10), ('Once', 11);

INSERT INTO categorias_recurso (nombre_categoria, descripcion) VALUES
('Libro', 'Libros de la biblioteca escolar'),
('Equipo de Laboratorio', 'Equipos científicos y de laboratorio (microscopios, balanzas, etc.)'),
('Equipo Deportivo', 'Equipos y materiales deportivos (balones, conos, etc.)'),
('Dispositivo Tecnológico', 'Tablets, calculadoras científicas, computadores portátiles'),
('Material Didáctico', 'Materiales educativos diversos (mapas, modelos, etc.)');

INSERT INTO conceptos_pago (codigo_concepto, nombre_concepto, descripcion, monto_base, es_recurrente) VALUES
('MAT', 'Matrícula', 'Pago anual de matrícula', 500000.00, FALSE),
('PENSION', 'Pensión Mensual', 'Pago mensual de pensión', 350000.00, TRUE),
('UNIFORME', 'Uniforme', 'Uniforme escolar completo', 250000.00, FALSE),
('TRANSPORTE', 'Transporte Escolar', 'Servicio de transporte mensual', 150000.00, TRUE),
('ALIMENTACION', 'Servicio de Alimentación', 'Almuerzo escolar mensual', 120000.00, TRUE),
('ACTIVIDAD', 'Actividad Especial', 'Salida pedagógica o actividad especial', 50000.00, FALSE),
('CERTIFICADO', 'Certificado', 'Certificado de estudio o constancia', 15000.00, FALSE);


-- Datos ficticios de entrega, sincronizados con seed_data.sql.


INSERT INTO estudiantes (numero_documento, primer_nombre, primer_apellido, fecha_nacimiento, genero, fecha_ingreso) VALUES
('LHSEED-E001', 'Ana', 'Rojas', '2019-05-10', 'F', '2026-01-15'),
('LHSEED-E002', 'Bruno', 'Moreno', '2019-05-10', 'M', '2026-01-15'),
('LHSEED-E003', 'Carla', 'Castro', '2019-05-10', 'F', '2026-01-15'),
('LHSEED-E004', 'Diego', 'Vargas', '2019-05-10', 'M', '2026-01-15'),
('LHSEED-E005', 'Elena', 'Mendoza', '2019-05-10', 'F', '2026-01-15'),
('LHSEED-E006', 'Fabio', 'Torres', '2019-05-10', 'M', '2026-01-15'),
('LHSEED-E007', 'Gloria', 'Herrera', '2019-05-10', 'F', '2026-01-15'),
('LHSEED-E008', 'Hugo', 'Ramirez', '2019-05-10', 'M', '2026-01-15'),
('LHSEED-E009', 'Ines', 'Suarez', '2019-05-10', 'F', '2026-01-15'),
('LHSEED-E010', 'Joel', 'Romero', '2019-05-10', 'M', '2026-01-15'),
('LHSEED-E011', 'Karla', 'Navarro', '2018-05-10', 'F', '2026-01-15'),
('LHSEED-E012', 'Luis', 'Medina', '2018-05-10', 'M', '2026-01-15'),
('LHSEED-E013', 'Marta', 'Ortega', '2018-05-10', 'F', '2026-01-15'),
('LHSEED-E014', 'Nicolas', 'Vega', '2018-05-10', 'M', '2026-01-15'),
('LHSEED-E015', 'Olga', 'Castillo', '2018-05-10', 'F', '2026-01-15'),
('LHSEED-E016', 'Pablo', 'Santos', '2018-05-10', 'M', '2026-01-15'),
('LHSEED-E017', 'Raquel', 'Cortes', '2018-05-10', 'F', '2026-01-15'),
('LHSEED-E018', 'Sergio', 'Reyes', '2018-05-10', 'M', '2026-01-15'),
('LHSEED-E019', 'Teresa', 'Molina', '2018-05-10', 'F', '2026-01-15'),
('LHSEED-E020', 'Ulises', 'Salazar', '2018-05-10', 'M', '2026-01-15'),
('LHSEED-E021', 'Valeria', 'Guzman', '2017-05-10', 'F', '2026-01-15'),
('LHSEED-E022', 'Walter', 'Pardo', '2017-05-10', 'M', '2026-01-15'),
('LHSEED-E023', 'Ximena', 'Rios', '2017-05-10', 'F', '2026-01-15'),
('LHSEED-E024', 'Yago', 'Leon', '2017-05-10', 'M', '2026-01-15'),
('LHSEED-E025', 'Zoe', 'Acosta', '2017-05-10', 'F', '2026-01-15'),
('LHSEED-E026', 'Andres', 'Diaz', '2017-05-10', 'M', '2026-01-15'),
('LHSEED-E027', 'Beatriz', 'Lopez', '2017-05-10', 'F', '2026-01-15'),
('LHSEED-E028', 'Camilo', 'Garcia', '2017-05-10', 'M', '2026-01-15'),
('LHSEED-E029', 'Diana', 'Martinez', '2017-05-10', 'F', '2026-01-15'),
('LHSEED-E030', 'Esteban', 'Rodriguez', '2017-05-10', 'M', '2026-01-15');

INSERT INTO acudientes (numero_documento, primer_nombre, primer_apellido, parentesco, telefono_principal, correo_electronico) VALUES
('LHSEED-A001', 'Ana', 'Rojas', 'Tutor Legal', '3000000001', 'tutor001@example.invalid'),
('LHSEED-A002', 'Bruno', 'Moreno', 'Tutor Legal', '3000000002', 'tutor002@example.invalid'),
('LHSEED-A003', 'Carla', 'Castro', 'Tutor Legal', '3000000003', 'tutor003@example.invalid'),
('LHSEED-A004', 'Diego', 'Vargas', 'Tutor Legal', '3000000004', 'tutor004@example.invalid'),
('LHSEED-A005', 'Elena', 'Mendoza', 'Tutor Legal', '3000000005', 'tutor005@example.invalid'),
('LHSEED-A006', 'Fabio', 'Torres', 'Tutor Legal', '3000000006', 'tutor006@example.invalid'),
('LHSEED-A007', 'Gloria', 'Herrera', 'Tutor Legal', '3000000007', 'tutor007@example.invalid'),
('LHSEED-A008', 'Hugo', 'Ramirez', 'Tutor Legal', '3000000008', 'tutor008@example.invalid'),
('LHSEED-A009', 'Ines', 'Suarez', 'Tutor Legal', '3000000009', 'tutor009@example.invalid'),
('LHSEED-A010', 'Joel', 'Romero', 'Tutor Legal', '3000000010', 'tutor010@example.invalid'),
('LHSEED-A011', 'Karla', 'Navarro', 'Tutor Legal', '3000000011', 'tutor011@example.invalid'),
('LHSEED-A012', 'Luis', 'Medina', 'Tutor Legal', '3000000012', 'tutor012@example.invalid'),
('LHSEED-A013', 'Marta', 'Ortega', 'Tutor Legal', '3000000013', 'tutor013@example.invalid'),
('LHSEED-A014', 'Nicolas', 'Vega', 'Tutor Legal', '3000000014', 'tutor014@example.invalid'),
('LHSEED-A015', 'Olga', 'Castillo', 'Tutor Legal', '3000000015', 'tutor015@example.invalid'),
('LHSEED-A016', 'Pablo', 'Santos', 'Tutor Legal', '3000000016', 'tutor016@example.invalid'),
('LHSEED-A017', 'Raquel', 'Cortes', 'Tutor Legal', '3000000017', 'tutor017@example.invalid'),
('LHSEED-A018', 'Sergio', 'Reyes', 'Tutor Legal', '3000000018', 'tutor018@example.invalid'),
('LHSEED-A019', 'Teresa', 'Molina', 'Tutor Legal', '3000000019', 'tutor019@example.invalid'),
('LHSEED-A020', 'Ulises', 'Salazar', 'Tutor Legal', '3000000020', 'tutor020@example.invalid'),
('LHSEED-A021', 'Valeria', 'Guzman', 'Tutor Legal', '3000000021', 'tutor021@example.invalid'),
('LHSEED-A022', 'Walter', 'Pardo', 'Tutor Legal', '3000000022', 'tutor022@example.invalid'),
('LHSEED-A023', 'Ximena', 'Rios', 'Tutor Legal', '3000000023', 'tutor023@example.invalid'),
('LHSEED-A024', 'Yago', 'Leon', 'Tutor Legal', '3000000024', 'tutor024@example.invalid'),
('LHSEED-A025', 'Zoe', 'Acosta', 'Tutor Legal', '3000000025', 'tutor025@example.invalid'),
('LHSEED-A026', 'Andres', 'Diaz', 'Tutor Legal', '3000000026', 'tutor026@example.invalid'),
('LHSEED-A027', 'Beatriz', 'Lopez', 'Tutor Legal', '3000000027', 'tutor027@example.invalid'),
('LHSEED-A028', 'Camilo', 'Garcia', 'Tutor Legal', '3000000028', 'tutor028@example.invalid'),
('LHSEED-A029', 'Diana', 'Martinez', 'Tutor Legal', '3000000029', 'tutor029@example.invalid'),
('LHSEED-A030', 'Esteban', 'Rodriguez', 'Tutor Legal', '3000000030', 'tutor030@example.invalid');

INSERT INTO profesores (numero_documento, primer_nombre, primer_apellido, correo_electronico, fecha_contratacion) VALUES
('LHSEED-P001', 'Ana', 'Rojas', 'profesor001@example.invalid', '2025-01-10'),
('LHSEED-P002', 'Bruno', 'Moreno', 'profesor002@example.invalid', '2025-01-10'),
('LHSEED-P003', 'Carla', 'Castro', 'profesor003@example.invalid', '2025-01-10'),
('LHSEED-P004', 'Diego', 'Vargas', 'profesor004@example.invalid', '2025-01-10'),
('LHSEED-P005', 'Elena', 'Mendoza', 'profesor005@example.invalid', '2025-01-10'),
('LHSEED-P006', 'Fabio', 'Torres', 'profesor006@example.invalid', '2025-01-10'),
('LHSEED-P007', 'Gloria', 'Herrera', 'profesor007@example.invalid', '2025-01-10'),
('LHSEED-P008', 'Hugo', 'Ramirez', 'profesor008@example.invalid', '2025-01-10'),
('LHSEED-P009', 'Ines', 'Suarez', 'profesor009@example.invalid', '2025-01-10'),
('LHSEED-P010', 'Joel', 'Romero', 'profesor010@example.invalid', '2025-01-10'),
('LHSEED-P011', 'Karla', 'Navarro', 'profesor011@example.invalid', '2025-01-10'),
('LHSEED-P012', 'Luis', 'Medina', 'profesor012@example.invalid', '2025-01-10'),
('LHSEED-P013', 'Marta', 'Ortega', 'profesor013@example.invalid', '2025-01-10'),
('LHSEED-P014', 'Nicolas', 'Vega', 'profesor014@example.invalid', '2025-01-10'),
('LHSEED-P015', 'Olga', 'Castillo', 'profesor015@example.invalid', '2025-01-10'),
('LHSEED-P016', 'Pablo', 'Santos', 'profesor016@example.invalid', '2025-01-10'),
('LHSEED-P017', 'Raquel', 'Cortes', 'profesor017@example.invalid', '2025-01-10'),
('LHSEED-P018', 'Sergio', 'Reyes', 'profesor018@example.invalid', '2025-01-10'),
('LHSEED-P019', 'Teresa', 'Molina', 'profesor019@example.invalid', '2025-01-10'),
('LHSEED-P020', 'Ulises', 'Salazar', 'profesor020@example.invalid', '2025-01-10'),
('LHSEED-P021', 'Valeria', 'Guzman', 'profesor021@example.invalid', '2025-01-10'),
('LHSEED-P022', 'Walter', 'Pardo', 'profesor022@example.invalid', '2025-01-10'),
('LHSEED-P023', 'Ximena', 'Rios', 'profesor023@example.invalid', '2025-01-10'),
('LHSEED-P024', 'Yago', 'Leon', 'profesor024@example.invalid', '2025-01-10'),
('LHSEED-P025', 'Zoe', 'Acosta', 'profesor025@example.invalid', '2025-01-10'),
('LHSEED-P026', 'Andres', 'Diaz', 'profesor026@example.invalid', '2025-01-10'),
('LHSEED-P027', 'Beatriz', 'Lopez', 'profesor027@example.invalid', '2025-01-10'),
('LHSEED-P028', 'Camilo', 'Garcia', 'profesor028@example.invalid', '2025-01-10'),
('LHSEED-P029', 'Diana', 'Martinez', 'profesor029@example.invalid', '2025-01-10'),
('LHSEED-P030', 'Esteban', 'Rodriguez', 'profesor030@example.invalid', '2025-01-10');

INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal)
SELECT e.id_estudiante, a.id_acudiente, TRUE
FROM estudiantes e JOIN acudientes a
  ON a.numero_documento = REPLACE(e.numero_documento, 'LHSEED-E', 'LHSEED-A')
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$';

INSERT INTO matriculas (id_estudiante, id_grado, año_academico, fecha_matricula)
SELECT e.id_estudiante, g.id_grado, 2026, '2026-01-15'
FROM estudiantes e CROSS JOIN grados g
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$' AND g.nivel_numerico = 1 + FLOOR((CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED) - 1) / 10);

INSERT INTO materias (codigo_materia, nombre_materia, id_grado, descripcion)
SELECT REPLACE(e.numero_documento, 'LHSEED-E', 'LHSEED-M'),
       CASE MOD(CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED) - 1, 10) WHEN 0 THEN 'Matemáticas' WHEN 1 THEN 'Lengua Castellana' WHEN 2 THEN 'Ciencias Naturales' WHEN 3 THEN 'Ciencias Sociales' WHEN 4 THEN 'Inglés' WHEN 5 THEN 'Educación Física' WHEN 6 THEN 'Educación Artística' WHEN 7 THEN 'Tecnología' WHEN 8 THEN 'Ética y Valores' WHEN 9 THEN 'Música' END, g.id_grado,
       'Asignatura del plan de estudios de primaria; datos ficticios de entrega'
FROM estudiantes e CROSS JOIN grados g
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$' AND g.nivel_numerico = 1 + FLOOR((CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED) - 1) / 10);

INSERT INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion)
SELECT p.id_profesor, m.id_materia, 2026, 'A'
FROM profesores p JOIN materias m
  ON m.codigo_materia = REPLACE(p.numero_documento, 'LHSEED-P', 'LHSEED-M')
WHERE p.numero_documento REGEXP '^LHSEED-P[0-9]{3}$';

INSERT INTO inscripciones_materia
    (id_estudiante, id_materia, año_academico, periodo_academico, seccion, fecha_inscripcion)
SELECT e.id_estudiante, m.id_materia, 2026, 'III', 'A', '2026-01-16'
FROM estudiantes e JOIN materias m
  ON m.codigo_materia = REPLACE(e.numero_documento, 'LHSEED-E', 'LHSEED-M')
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$';

INSERT INTO registros_academicos
    (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico,
     valor_nota, observaciones, fecha_registro)
SELECT e.id_estudiante, m.id_materia, p.id_profesor, 2026, 'III',
       3.0 + MOD(CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED), 21) / 10.0,
       'LHSEED: nota ficticia', '2026-09-18'
FROM estudiantes e
JOIN materias m ON m.codigo_materia = REPLACE(e.numero_documento, 'LHSEED-E', 'LHSEED-M')
JOIN profesores p ON p.numero_documento = REPLACE(e.numero_documento, 'LHSEED-E', 'LHSEED-P')
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$';

INSERT INTO pagos
    (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago,
     metodo_pago, numero_recibo, año_academico, periodo_academico, notas)
SELECT e.id_estudiante, a.id_acudiente, c.id_concepto, 350000.00, '2026-09-18',
       'Transferencia', REPLACE(e.numero_documento, 'LHSEED-E', 'LHSEED-REC'),
       2026, 'III', 'LHSEED: pago ficticio sin factura ni aplicación automática'
FROM estudiantes e
JOIN acudientes a ON a.numero_documento = REPLACE(e.numero_documento, 'LHSEED-E', 'LHSEED-A')
CROSS JOIN conceptos_pago c
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$' AND c.codigo_concepto = 'PENSION';

INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima)
SELECT CONCAT('LHSEED-Club-', RIGHT(p.numero_documento, 3)),
       CASE CAST(RIGHT(p.numero_documento, 3) AS UNSIGNED) WHEN 1 THEN 'Club de Ajedrez' WHEN 2 THEN 'Club de Robótica' WHEN 3 THEN 'Club de Lectura' WHEN 4 THEN 'Club de Teatro' WHEN 5 THEN 'Club de Coro' WHEN 6 THEN 'Club de Danza' WHEN 7 THEN 'Club de Fútbol' WHEN 8 THEN 'Club de Baloncesto' WHEN 9 THEN 'Club de Voleibol' WHEN 10 THEN 'Club de Atletismo' WHEN 11 THEN 'Club de Pintura' WHEN 12 THEN 'Club de Fotografía' WHEN 13 THEN 'Club de Periodismo' WHEN 14 THEN 'Club de Astronomía' WHEN 15 THEN 'Club de Huerta Escolar' WHEN 16 THEN 'Club de Ciencias' WHEN 17 THEN 'Club de Matemáticas' WHEN 18 THEN 'Club de Inglés' WHEN 19 THEN 'Club de Programación' WHEN 20 THEN 'Club de Debate' WHEN 21 THEN 'Club de Cine' WHEN 22 THEN 'Club de Origami' WHEN 23 THEN 'Club de Escultura' WHEN 24 THEN 'Club de Música Andina' WHEN 25 THEN 'Club de Percusión' WHEN 26 THEN 'Club de Natación' WHEN 27 THEN 'Club de Patinaje' WHEN 28 THEN 'Club de Reciclaje' WHEN 29 THEN 'Club de Senderismo' WHEN 30 THEN 'Club de Escritura' END, p.id_profesor, 15
FROM profesores p WHERE p.numero_documento REGEXP '^LHSEED-P[0-9]{3}$';

INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso)
SELECT e.id_estudiante, c.id_club, '2026-02-01'
FROM estudiantes e JOIN clubes c
  ON c.nombre_club = CONCAT('LHSEED-Club-', RIGHT(e.numero_documento, 3))
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$';

INSERT INTO recursos
    (codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion, ubicacion_fisica)
SELECT REPLACE(e.numero_documento, 'LHSEED-E', 'LHSEED-R'),
       CONCAT(CASE MOD(CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED) - 1, 10) WHEN 0 THEN 'El principito' WHEN 1 THEN 'La peor señora del mundo' WHEN 2 THEN 'Cuentos de la selva' WHEN 3 THEN 'Rafael Pombo: poemas infantiles' WHEN 4 THEN 'Fábulas de Esopo' WHEN 5 THEN 'Platero y yo' WHEN 6 THEN 'El libro de la selva' WHEN 7 THEN 'Alicia en el país de las maravillas' WHEN 8 THEN 'Mitos y leyendas de Colombia' WHEN 9 THEN 'Atlas escolar de Colombia' END, ' - ejemplar ', RIGHT(e.numero_documento, 3)), c.id_categoria,
       'Recurso ficticio de demostración', '2026-01-10', 'Biblioteca de prueba'
FROM estudiantes e CROSS JOIN categorias_recurso c
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$' AND c.nombre_categoria = 'Libro';


COMMIT;
