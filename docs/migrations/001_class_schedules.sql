USE erp_little_horizons;

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
