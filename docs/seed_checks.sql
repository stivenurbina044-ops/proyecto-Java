-- Solo lectura. Ejecutar en la MISMA conexión que seed_data.sql.
USE erp_little_horizons;

-- Conteos esperados del lote ficticio en una carga nueva.
SELECT 'estudiantes' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM estudiantes WHERE numero_documento REGEXP '^1026[0-9]{3}$'
UNION ALL
SELECT 'acudientes' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM acudientes WHERE numero_documento REGEXP '^5226[0-9]{3}$'
UNION ALL
SELECT 'profesores' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM profesores WHERE numero_documento REGEXP '^8026[0-9]{3}$'
UNION ALL
SELECT 'materias' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM materias WHERE codigo_materia REGEXP '^LHSEED-M[0-9]{3}$'
UNION ALL
SELECT 'registros_academicos' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM registros_academicos WHERE observaciones = 'LHSEED: nota ficticia'
UNION ALL
SELECT 'pagos' AS tabla, COUNT(*) AS registros_lote, 35 AS esperado FROM pagos WHERE numero_recibo REGEXP '^LHSEED-REC(-OCT)?[0-9]{3}$'
UNION ALL
SELECT 'clubes' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM clubes WHERE nombre_club REGEXP '^LHSEED-Club-[0-9]{3}$'
UNION ALL
SELECT 'recursos' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM recursos WHERE codigo_recurso REGEXP '^LHSEED-R[0-9]{3}$'
UNION ALL SELECT 'horarios_clase', COUNT(*), 30 FROM horarios_clase h
JOIN asignaciones_profesor a USING (id_asignacion) JOIN materias m USING (id_materia)
WHERE m.codigo_materia REGEXP '^LHSEED-M[0-9]{3}$' AND a.año_academico = 2026 AND a.seccion = 'A'
UNION ALL SELECT 'asistencias', COUNT(*), 30 FROM asistencias
WHERE observaciones = 'LHSEED: asistencia ficticia'
UNION ALL SELECT 'eventos', COUNT(*), 30 FROM eventos
WHERE nombre_evento REGEXP '^LHSEED-Evento-[0-9]{3}$'
UNION ALL SELECT 'participantes_evento', COUNT(*), 30 FROM participantes_evento JOIN eventos USING (id_evento)
WHERE nombre_evento REGEXP '^LHSEED-Evento-[0-9]{3}$'
UNION ALL SELECT 'prestamos_estudiante', COUNT(*), 30 FROM prestamos_estudiante
WHERE observaciones = 'LHSEED: préstamo de estudiante'
UNION ALL SELECT 'prestamos_profesor', COUNT(*), 30 FROM prestamos_profesor
WHERE observaciones = 'LHSEED: préstamo de profesor'
UNION ALL SELECT 'tarifas_estudiante', COUNT(*), 30 FROM tarifas_estudiante
WHERE motivo = 'LHSEED: tarifa ficticia de pensión'
UNION ALL SELECT 'cobros_estudiante', COUNT(*), 35 FROM cobros_estudiante
WHERE referencia IN ('LHSEED-PENSION-SEP', 'LHSEED-PENSION-OCT')
UNION ALL SELECT 'aplicaciones_pago', COUNT(*), 30 FROM aplicaciones_pago JOIN pagos USING (id_pago)
WHERE numero_recibo REGEXP '^LHSEED-REC(-OCT)?[0-9]{3}$';

-- Cada relación debe aportar 30 filas.
SELECT 'vinculos' AS relacion, COUNT(*) AS total, 30 AS esperado
FROM estudiante_acudiente ea JOIN estudiantes e USING (id_estudiante)
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$'
UNION ALL
SELECT 'matriculas', COUNT(*), 30 FROM matriculas ma JOIN estudiantes e USING (id_estudiante)
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$'
UNION ALL
SELECT 'asignaciones', COUNT(*), 30 FROM asignaciones_profesor a JOIN profesores p USING (id_profesor)
WHERE p.numero_documento REGEXP '^8026[0-9]{3}$'
UNION ALL
SELECT 'inscripciones', COUNT(*), 30 FROM inscripciones_materia i JOIN estudiantes e USING (id_estudiante)
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$'
UNION ALL
SELECT 'membresias', COUNT(*), 30 FROM membresias_club mc JOIN estudiantes e USING (id_estudiante)
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$';

-- Debe devolver cero filas: notas sin matrícula, inscripción o asignación coherente.
SELECT r.id_registro
FROM registros_academicos r
JOIN estudiantes e ON e.id_estudiante = r.id_estudiante
JOIN materias m ON m.id_materia = r.id_materia
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$'
  AND (NOT EXISTS (
      SELECT 1 FROM matriculas ma WHERE ma.id_estudiante = r.id_estudiante
      AND ma.año_academico = r.año_academico AND ma.id_grado = m.id_grado
  ) OR NOT EXISTS (
      SELECT 1 FROM inscripciones_materia i WHERE i.id_estudiante = r.id_estudiante
      AND i.id_materia = r.id_materia AND i.año_academico = r.año_academico
      AND i.periodo_academico = r.periodo_academico AND i.seccion = 'A'
  ) OR NOT EXISTS (
      SELECT 1 FROM asignaciones_profesor a WHERE a.id_profesor = r.id_profesor
      AND a.id_materia = r.id_materia AND a.año_academico = r.año_academico
      AND a.seccion = 'A'
  ));

-- Debe devolver cero filas: pagos sin vínculo de acudiente.
SELECT p.id_pago FROM pagos p
WHERE p.numero_recibo REGEXP '^LHSEED-REC(-OCT)?[0-9]{3}$'
AND NOT EXISTS (SELECT 1 FROM estudiante_acudiente ea
    WHERE ea.id_estudiante = p.id_estudiante AND ea.id_acudiente = p.id_acudiente);

-- Debe devolver cero filas: cobros de prueba incompatibles o con sobreabono.
SELECT c.id_cobro
FROM cobros_estudiante c
JOIN aplicaciones_pago ap USING (id_cobro)
JOIN pagos p USING (id_pago)
WHERE c.referencia IN ('LHSEED-PENSION-SEP', 'LHSEED-PENSION-OCT')
GROUP BY c.id_cobro, c.monto_cobrado
HAVING SUM(p.monto_pagado) > c.monto_cobrado
    OR SUM(p.id_estudiante <> c.id_estudiante OR p.id_concepto <> c.id_concepto
        OR NOT (p.año_academico <=> c.año_academico)
        OR NOT (p.periodo_academico <=> c.periodo_academico)) > 0;

-- Debe devolver cero filas: estado de inventario incompatible con préstamos abiertos.
SELECT r.id_recurso
FROM recursos r
WHERE r.codigo_recurso REGEXP '^LHSEED-R[0-9]{3}$'
  AND (r.estado_recurso = 'Prestado') <> (
      EXISTS (SELECT 1 FROM prestamos_profesor pp WHERE pp.id_recurso = r.id_recurso
              AND pp.estado_prestamo IN ('Activo', 'Vencido'))
      OR EXISTS (SELECT 1 FROM prestamos_estudiante pe WHERE pe.id_recurso = r.id_recurso
                 AND pe.estado_prestamo IN ('Activo', 'Vencido'))
  );
