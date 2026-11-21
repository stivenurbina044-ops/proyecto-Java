-- Ejecutar después de cargar el anexo del examen.
-- Usa al estudiante 3, su matrícula de 2025 y el docente de su nota de Matemáticas I.
USE erp_little_horizons_check;

START TRANSACTION;

INSERT INTO matriculas (id_estudiante, id_grado, año_academico, fecha_matricula)
SELECT original.id_estudiante, original.id_grado, fixture.año, MAKEDATE(fixture.año, 15)
FROM matriculas AS original
JOIN estudiantes AS estudiante ON estudiante.id_estudiante = original.id_estudiante
CROSS JOIN (SELECT 2024 AS año UNION ALL SELECT 2026) AS fixture
LEFT JOIN matriculas AS existente
    ON existente.id_estudiante = original.id_estudiante AND existente.año_academico = fixture.año
WHERE original.id_estudiante = 3
  AND estudiante.numero_documento = '1023456791'
  AND original.año_academico = 2025
  AND existente.id_matricula IS NULL;

INSERT INTO registros_academicos
    (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico, valor_nota, fecha_registro)
SELECT original.id_estudiante, materia.id_materia, original.id_profesor,
       fixture.año, fixture.periodo, fixture.nota, MAKEDATE(fixture.año, 90)
FROM registros_academicos AS original
JOIN estudiantes AS estudiante ON estudiante.id_estudiante = original.id_estudiante
JOIN materias AS matematicas
    ON matematicas.id_materia = original.id_materia AND matematicas.codigo_materia = 'MAT-10'
CROSS JOIN (
    SELECT 2024 AS año, 'MAT-10' AS codigo, 'I' AS periodo, 3.0 AS nota
    UNION ALL SELECT 2024, 'MAT-10', 'II', 3.4
    UNION ALL SELECT 2024, 'ING-10', 'I', 4.0
    UNION ALL SELECT 2026, 'MAT-10', 'I', 4.6
    UNION ALL SELECT 2026, 'MAT-10', 'II', 4.8
    UNION ALL SELECT 2026, 'BIO-10', 'I', 4.5
) AS fixture
JOIN materias AS materia ON materia.codigo_materia = fixture.codigo
JOIN matriculas AS matricula
    ON matricula.id_estudiante = original.id_estudiante
    AND matricula.año_academico = fixture.año
    AND matricula.id_grado = materia.id_grado
LEFT JOIN registros_academicos AS existente
    ON existente.id_estudiante = original.id_estudiante
    AND existente.id_materia = materia.id_materia
    AND existente.año_academico = fixture.año
    AND existente.periodo_academico = fixture.periodo
WHERE original.id_estudiante = 3
  AND estudiante.numero_documento = '1023456791'
  AND original.año_academico = 2025
  AND original.periodo_academico = 'I'
  AND existente.id_registro IS NULL;

COMMIT;

SELECT año_academico, COUNT(DISTINCT id_materia) AS materias, COUNT(*) AS notas
FROM registros_academicos
WHERE id_estudiante = 3 AND año_academico IN (2024, 2025, 2026)
GROUP BY año_academico
ORDER BY año_academico;
