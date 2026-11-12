-- Solo lectura. Ejecutar en la MISMA conexión que seed_data.sql.
USE erp_little_horizons;

-- Cada tabla debe aportar exactamente 30 registros del lote ficticio.
SELECT 'estudiantes' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM estudiantes WHERE numero_documento REGEXP '^LHSEED-E[0-9]{3}$'
UNION ALL
SELECT 'acudientes' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM acudientes WHERE numero_documento REGEXP '^LHSEED-A[0-9]{3}$'
UNION ALL
SELECT 'profesores' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM profesores WHERE numero_documento REGEXP '^LHSEED-P[0-9]{3}$'
UNION ALL
SELECT 'materias' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM materias WHERE codigo_materia REGEXP '^LHSEED-M[0-9]{3}$'
UNION ALL
SELECT 'registros_academicos' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM registros_academicos WHERE observaciones = 'LHSEED: nota ficticia'
UNION ALL
SELECT 'pagos' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM pagos WHERE numero_recibo REGEXP '^LHSEED-REC[0-9]{3}$'
UNION ALL
SELECT 'clubes' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM clubes WHERE nombre_club REGEXP '^LHSEED-Club-[0-9]{3}$'
UNION ALL
SELECT 'recursos' AS tabla, COUNT(*) AS registros_lote, 30 AS esperado FROM recursos WHERE codigo_recurso REGEXP '^LHSEED-R[0-9]{3}$';

-- Cada relación debe aportar 30 filas.
SELECT 'vinculos' AS relacion, COUNT(*) AS total
FROM estudiante_acudiente ea JOIN estudiantes e USING (id_estudiante)
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$'
UNION ALL
SELECT 'matriculas', COUNT(*) FROM matriculas ma JOIN estudiantes e USING (id_estudiante)
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$'
UNION ALL
SELECT 'asignaciones', COUNT(*) FROM asignaciones_profesor a JOIN profesores p USING (id_profesor)
WHERE p.numero_documento REGEXP '^LHSEED-P[0-9]{3}$'
UNION ALL
SELECT 'inscripciones', COUNT(*) FROM inscripciones_materia i JOIN estudiantes e USING (id_estudiante)
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$'
UNION ALL
SELECT 'membresias', COUNT(*) FROM membresias_club mc JOIN estudiantes e USING (id_estudiante)
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$';

-- Debe devolver cero filas: notas sin matrícula, inscripción o asignación coherente.
SELECT r.id_registro
FROM registros_academicos r
JOIN estudiantes e ON e.id_estudiante = r.id_estudiante
JOIN materias m ON m.id_materia = r.id_materia
WHERE e.numero_documento REGEXP '^LHSEED-E[0-9]{3}$'
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
WHERE p.numero_recibo REGEXP '^LHSEED-REC[0-9]{3}$'
AND NOT EXISTS (SELECT 1 FROM estudiante_acudiente ea
    WHERE ea.id_estudiante = p.id_estudiante AND ea.id_acudiente = p.id_acudiente);
