-- Consultas de revisión: no modifican datos.
USE erp_little_horizons;

SELECT TABLE_NAME AS tabla, TABLE_COMMENT AS descripcion
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'erp_little_horizons' AND TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;

SELECT 'estudiantes' AS tabla, COUNT(*) AS total FROM estudiantes
UNION ALL SELECT 'acudientes', COUNT(*) FROM acudientes
UNION ALL SELECT 'profesores', COUNT(*) FROM profesores
UNION ALL SELECT 'materias', COUNT(*) FROM materias
UNION ALL SELECT 'registros_academicos', COUNT(*) FROM registros_academicos
UNION ALL SELECT 'pagos', COUNT(*) FROM pagos
UNION ALL SELECT 'clubes', COUNT(*) FROM clubes
UNION ALL SELECT 'recursos', COUNT(*) FROM recursos;

SELECT e.id_estudiante, e.numero_documento,
       e.primer_nombre AS estudiante, a.id_acudiente,
       a.primer_nombre AS acudiente, ea.es_principal, ea.es_contacto_emergencia
FROM estudiantes e
LEFT JOIN estudiante_acudiente ea ON ea.id_estudiante = e.id_estudiante
LEFT JOIN acudientes a ON a.id_acudiente = ea.id_acudiente
ORDER BY e.id_estudiante, a.id_acudiente;

SELECT id_profesor, numero_documento, primer_nombre, primer_apellido,
       fecha_contratacion, estado
FROM profesores
ORDER BY id_profesor DESC;

SELECT a.id_asignacion, p.primer_nombre AS profesor,
       p.primer_apellido, m.codigo_materia, m.nombre_materia,
       a.año_academico, a.seccion
FROM asignaciones_profesor a
JOIN profesores p ON p.id_profesor = a.id_profesor
JOIN materias m ON m.id_materia = a.id_materia
ORDER BY a.id_asignacion DESC;

SELECT h.id_horario, h.id_asignacion, a.id_profesor, a.año_academico,
       m.codigo_materia, a.seccion, h.dia_semana, h.hora_inicio, h.hora_fin
FROM horarios_clase h
JOIN asignaciones_profesor a ON a.id_asignacion = h.id_asignacion
JOIN materias m ON m.id_materia = a.id_materia
ORDER BY a.año_academico DESC, a.id_profesor, h.dia_semana, h.hora_inicio;

-- El resultado debería estar vacío: profesores activos contratados en el futuro.
SELECT id_profesor, numero_documento, fecha_contratacion, estado
FROM profesores
WHERE estado = 'Activo' AND fecha_contratacion > CURRENT_DATE;

-- El resultado debería estar vacío: asignaciones duplicadas, incluidas secciones NULL.
SELECT id_profesor, id_materia, año_academico, seccion, COUNT(*) AS total
FROM asignaciones_profesor
GROUP BY id_profesor, id_materia, año_academico, seccion
HAVING COUNT(*) > 1;

-- El resultado debería estar vacío: cruces de horarios de un profesor en el mismo año.
SELECT h1.id_horario AS primer_horario, h2.id_horario AS segundo_horario,
       a1.id_profesor, a1.año_academico, h1.dia_semana
FROM horarios_clase h1
JOIN asignaciones_profesor a1 ON a1.id_asignacion = h1.id_asignacion
JOIN asignaciones_profesor a2 ON a2.id_profesor = a1.id_profesor
                            AND a2.año_academico = a1.año_academico
JOIN horarios_clase h2 ON h2.id_asignacion = a2.id_asignacion
WHERE h1.id_horario < h2.id_horario
  AND h1.dia_semana = h2.dia_semana
  AND h1.hora_inicio < h2.hora_fin
  AND h1.hora_fin > h2.hora_inicio;

-- Historial reciente de asistencia.
SELECT a.id_asistencia, e.numero_documento, m.nombre_materia,
       a.fecha_asistencia, a.estado_asistencia, a.registrado_por, a.observaciones
FROM asistencias a
JOIN estudiantes e ON e.id_estudiante = a.id_estudiante
JOIN materias m ON m.id_materia = a.id_materia
ORDER BY a.fecha_asistencia DESC, a.id_asistencia DESC;

-- Clubes y ocupación actual.
SELECT c.id_club, c.nombre_club, c.capacidad_maxima, c.esta_activo,
       COUNT(mc.id_membresia) AS miembros_activos
FROM clubes c
LEFT JOIN membresias_club mc ON mc.id_club = c.id_club AND mc.estado_membresia = 'Activa'
GROUP BY c.id_club, c.nombre_club, c.capacidad_maxima, c.esta_activo
ORDER BY c.id_club;

SELECT mc.id_membresia, e.numero_documento, c.nombre_club,
       mc.fecha_ingreso, mc.estado_membresia, mc.fecha_retiro, mc.motivo_retiro
FROM membresias_club mc
JOIN estudiantes e ON e.id_estudiante = mc.id_estudiante
JOIN clubes c ON c.id_club = mc.id_club
ORDER BY mc.id_membresia DESC;

-- Calendario y participaciones en eventos.
SELECT id_evento, nombre_evento, tipo_evento, fecha_evento, lugar,
       tiene_competencia, id_organizador, esta_activo
FROM eventos
ORDER BY fecha_evento, id_evento;

SELECT pe.id_participacion, v.nombre_evento, v.fecha_evento,
       e.numero_documento, pe.rol_participacion, pe.posicion_obtenida, pe.observaciones
FROM participantes_evento pe
JOIN eventos v ON v.id_evento = pe.id_evento
JOIN estudiantes e ON e.id_estudiante = pe.id_estudiante
ORDER BY pe.id_participacion DESC;

-- Inventario de recursos.
SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, c.nombre_categoria,
       r.ubicacion_fisica, r.estado_recurso
FROM recursos r JOIN categorias_recurso c ON c.id_categoria = r.id_categoria
ORDER BY r.id_recurso;

-- Préstamos: conservar el tipo junto al ID, ya que las dos tablas tienen IDs propios.
SELECT 'Estudiante' AS tipo, id_prestamo, id_recurso, id_estudiante AS id_solicitante,
       fecha_prestamo, fecha_devolucion_esperada, fecha_devolucion_real, estado_prestamo
FROM prestamos_estudiante
UNION ALL
SELECT 'Profesor', id_prestamo, id_recurso, id_profesor,
       fecha_prestamo, fecha_devolucion_esperada, fecha_devolucion_real, estado_prestamo
FROM prestamos_profesor;

-- Conceptos y pagos registrados.
SELECT id_concepto, codigo_concepto, nombre_concepto, monto_base, es_recurrente, esta_activo
FROM conceptos_pago ORDER BY id_concepto;

SELECT p.id_pago, e.numero_documento AS documento_estudiante,
       a.numero_documento AS documento_acudiente, c.nombre_concepto,
       p.monto_pagado, p.fecha_pago, p.metodo_pago, p.numero_recibo,
       p.año_academico, p.periodo_academico, p.notas
FROM pagos p
JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
LEFT JOIN acudientes a ON a.id_acudiente = p.id_acudiente
JOIN conceptos_pago c ON c.id_concepto = p.id_concepto
ORDER BY p.id_pago DESC;

-- Facturas PDF académicas asociadas a pagos.
SELECT id_factura, numero_factura, id_pago, fecha_emision, cufe,
       nombre_archivo_pdf, ruta_archivo
FROM facturas
ORDER BY id_factura DESC;

-- El resultado debería estar vacío: varias facturas para un mismo pago.
SELECT id_pago, COUNT(*) AS total
FROM facturas
GROUP BY id_pago
HAVING COUNT(*) > 1;

-- Tarifas personalizadas por estudiante y año.
SELECT e.numero_documento, c.nombre_concepto, t.año_academico, t.monto_personalizado, t.motivo
FROM tarifas_estudiante t
JOIN estudiantes e ON e.id_estudiante = t.id_estudiante
JOIN conceptos_pago c ON c.id_concepto = t.id_concepto
ORDER BY e.numero_documento, t.año_academico, c.nombre_concepto;

-- Saldo por cobro: solo se descuentan pagos expresamente aplicados.
SELECT b.id_cobro, e.numero_documento, c.nombre_concepto, b.referencia,
       b.año_academico, b.periodo_academico, b.monto_cobrado, b.fecha_vencimiento,
       COALESCE(a.pagado, 0) AS pagado, b.monto_cobrado - COALESCE(a.pagado, 0) AS saldo
FROM cobros_estudiante b
JOIN estudiantes e ON e.id_estudiante = b.id_estudiante
JOIN conceptos_pago c ON c.id_concepto = b.id_concepto
LEFT JOIN (
    SELECT ap.id_cobro, SUM(p.monto_pagado) AS pagado
    FROM aplicaciones_pago ap JOIN pagos p ON p.id_pago = ap.id_pago
    GROUP BY ap.id_cobro
) a ON a.id_cobro = b.id_cobro
ORDER BY b.id_cobro;

-- Pagos todavía no aplicados a cobros: no crear otro pago para aplicarlos.
SELECT p.id_pago, p.numero_recibo, p.id_estudiante, p.id_concepto,
       p.monto_pagado, p.año_academico, p.periodo_academico
FROM pagos p LEFT JOIN aplicaciones_pago ap ON ap.id_pago = p.id_pago
WHERE ap.id_pago IS NULL
ORDER BY p.id_pago;

-- Referencia de promedios por grado/año/período, sin tomar faltantes como cero.
SELECT enrollment.id_grado, r.año_academico, r.periodo_academico,
       m.codigo_materia, m.nombre_materia, COUNT(*) AS estudiantes_con_nota,
       ROUND(AVG(r.valor_nota), 1) AS promedio
FROM registros_academicos r
JOIN materias m ON m.id_materia = r.id_materia
JOIN matriculas enrollment ON enrollment.id_estudiante = r.id_estudiante
     AND enrollment.año_academico = r.año_academico AND enrollment.id_grado = m.id_grado
GROUP BY enrollment.id_grado, r.año_academico, r.periodo_academico,
         m.id_materia, m.codigo_materia, m.nombre_materia
ORDER BY r.año_academico, enrollment.id_grado, m.codigo_materia, r.periodo_academico;
