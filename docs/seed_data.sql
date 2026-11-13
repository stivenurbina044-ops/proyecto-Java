-- Datos completamente ficticios para una copia de pruebas de Little Horizons.
-- Solo para una base existente con esquema y catálogos, pero sin lote LHSEED.
-- database.sql ya incluye este lote: no ejecutar ambos ni repetir esta carga.
-- DBeaver: detener al primer error y ejecutar ROLLBACK en la misma conexión.
-- No usa IDs fijos, INSERT IGNORE, borrados ni cambios a registros existentes.
USE erp_little_horizons;
START TRANSACTION;

INSERT INTO estudiantes (numero_documento, primer_nombre, primer_apellido, fecha_nacimiento, genero, fecha_ingreso) VALUES
('1026001', 'Ana', 'Rojas', '2019-05-10', 'F', '2026-01-15'),
('1026002', 'Bruno', 'Moreno', '2019-05-10', 'M', '2026-01-15'),
('1026003', 'Carla', 'Castro', '2019-05-10', 'F', '2026-01-15'),
('1026004', 'Diego', 'Vargas', '2019-05-10', 'M', '2026-01-15'),
('1026005', 'Elena', 'Mendoza', '2019-05-10', 'F', '2026-01-15'),
('1026006', 'Fabio', 'Torres', '2019-05-10', 'M', '2026-01-15'),
('1026007', 'Gloria', 'Herrera', '2019-05-10', 'F', '2026-01-15'),
('1026008', 'Hugo', 'Ramirez', '2019-05-10', 'M', '2026-01-15'),
('1026009', 'Ines', 'Suarez', '2019-05-10', 'F', '2026-01-15'),
('1026010', 'Joel', 'Romero', '2019-05-10', 'M', '2026-01-15'),
('1026011', 'Karla', 'Navarro', '2018-05-10', 'F', '2026-01-15'),
('1026012', 'Luis', 'Medina', '2018-05-10', 'M', '2026-01-15'),
('1026013', 'Marta', 'Ortega', '2018-05-10', 'F', '2026-01-15'),
('1026014', 'Nicolas', 'Vega', '2018-05-10', 'M', '2026-01-15'),
('1026015', 'Olga', 'Castillo', '2018-05-10', 'F', '2026-01-15'),
('1026016', 'Pablo', 'Santos', '2018-05-10', 'M', '2026-01-15'),
('1026017', 'Raquel', 'Cortes', '2018-05-10', 'F', '2026-01-15'),
('1026018', 'Sergio', 'Reyes', '2018-05-10', 'M', '2026-01-15'),
('1026019', 'Teresa', 'Molina', '2018-05-10', 'F', '2026-01-15'),
('1026020', 'Ulises', 'Salazar', '2018-05-10', 'M', '2026-01-15'),
('1026021', 'Valeria', 'Guzman', '2017-05-10', 'F', '2026-01-15'),
('1026022', 'Walter', 'Pardo', '2017-05-10', 'M', '2026-01-15'),
('1026023', 'Ximena', 'Rios', '2017-05-10', 'F', '2026-01-15'),
('1026024', 'Yago', 'Leon', '2017-05-10', 'M', '2026-01-15'),
('1026025', 'Zoe', 'Acosta', '2017-05-10', 'F', '2026-01-15'),
('1026026', 'Andres', 'Diaz', '2017-05-10', 'M', '2026-01-15'),
('1026027', 'Beatriz', 'Lopez', '2017-05-10', 'F', '2026-01-15'),
('1026028', 'Camilo', 'Garcia', '2017-05-10', 'M', '2026-01-15'),
('1026029', 'Diana', 'Martinez', '2017-05-10', 'F', '2026-01-15'),
('1026030', 'Esteban', 'Rodriguez', '2017-05-10', 'M', '2026-01-15');

INSERT INTO acudientes (numero_documento, primer_nombre, primer_apellido, parentesco, telefono_principal, correo_electronico) VALUES
('5226001', 'Ana', 'Rojas', 'Tutor Legal', '3000000001', 'tutor001@example.invalid'),
('5226002', 'Bruno', 'Moreno', 'Tutor Legal', '3000000002', 'tutor002@example.invalid'),
('5226003', 'Carla', 'Castro', 'Tutor Legal', '3000000003', 'tutor003@example.invalid'),
('5226004', 'Diego', 'Vargas', 'Tutor Legal', '3000000004', 'tutor004@example.invalid'),
('5226005', 'Elena', 'Mendoza', 'Tutor Legal', '3000000005', 'tutor005@example.invalid'),
('5226006', 'Fabio', 'Torres', 'Tutor Legal', '3000000006', 'tutor006@example.invalid'),
('5226007', 'Gloria', 'Herrera', 'Tutor Legal', '3000000007', 'tutor007@example.invalid'),
('5226008', 'Hugo', 'Ramirez', 'Tutor Legal', '3000000008', 'tutor008@example.invalid'),
('5226009', 'Ines', 'Suarez', 'Tutor Legal', '3000000009', 'tutor009@example.invalid'),
('5226010', 'Joel', 'Romero', 'Tutor Legal', '3000000010', 'tutor010@example.invalid'),
('5226011', 'Karla', 'Navarro', 'Tutor Legal', '3000000011', 'tutor011@example.invalid'),
('5226012', 'Luis', 'Medina', 'Tutor Legal', '3000000012', 'tutor012@example.invalid'),
('5226013', 'Marta', 'Ortega', 'Tutor Legal', '3000000013', 'tutor013@example.invalid'),
('5226014', 'Nicolas', 'Vega', 'Tutor Legal', '3000000014', 'tutor014@example.invalid'),
('5226015', 'Olga', 'Castillo', 'Tutor Legal', '3000000015', 'tutor015@example.invalid'),
('5226016', 'Pablo', 'Santos', 'Tutor Legal', '3000000016', 'tutor016@example.invalid'),
('5226017', 'Raquel', 'Cortes', 'Tutor Legal', '3000000017', 'tutor017@example.invalid'),
('5226018', 'Sergio', 'Reyes', 'Tutor Legal', '3000000018', 'tutor018@example.invalid'),
('5226019', 'Teresa', 'Molina', 'Tutor Legal', '3000000019', 'tutor019@example.invalid'),
('5226020', 'Ulises', 'Salazar', 'Tutor Legal', '3000000020', 'tutor020@example.invalid'),
('5226021', 'Valeria', 'Guzman', 'Tutor Legal', '3000000021', 'tutor021@example.invalid'),
('5226022', 'Walter', 'Pardo', 'Tutor Legal', '3000000022', 'tutor022@example.invalid'),
('5226023', 'Ximena', 'Rios', 'Tutor Legal', '3000000023', 'tutor023@example.invalid'),
('5226024', 'Yago', 'Leon', 'Tutor Legal', '3000000024', 'tutor024@example.invalid'),
('5226025', 'Zoe', 'Acosta', 'Tutor Legal', '3000000025', 'tutor025@example.invalid'),
('5226026', 'Andres', 'Diaz', 'Tutor Legal', '3000000026', 'tutor026@example.invalid'),
('5226027', 'Beatriz', 'Lopez', 'Tutor Legal', '3000000027', 'tutor027@example.invalid'),
('5226028', 'Camilo', 'Garcia', 'Tutor Legal', '3000000028', 'tutor028@example.invalid'),
('5226029', 'Diana', 'Martinez', 'Tutor Legal', '3000000029', 'tutor029@example.invalid'),
('5226030', 'Esteban', 'Rodriguez', 'Tutor Legal', '3000000030', 'tutor030@example.invalid');

INSERT INTO profesores (numero_documento, primer_nombre, primer_apellido, correo_electronico, fecha_contratacion) VALUES
('8026001', 'Ana', 'Rojas', 'profesor001@example.invalid', '2025-01-10'),
('8026002', 'Bruno', 'Moreno', 'profesor002@example.invalid', '2025-01-10'),
('8026003', 'Carla', 'Castro', 'profesor003@example.invalid', '2025-01-10'),
('8026004', 'Diego', 'Vargas', 'profesor004@example.invalid', '2025-01-10'),
('8026005', 'Elena', 'Mendoza', 'profesor005@example.invalid', '2025-01-10'),
('8026006', 'Fabio', 'Torres', 'profesor006@example.invalid', '2025-01-10'),
('8026007', 'Gloria', 'Herrera', 'profesor007@example.invalid', '2025-01-10'),
('8026008', 'Hugo', 'Ramirez', 'profesor008@example.invalid', '2025-01-10'),
('8026009', 'Ines', 'Suarez', 'profesor009@example.invalid', '2025-01-10'),
('8026010', 'Joel', 'Romero', 'profesor010@example.invalid', '2025-01-10'),
('8026011', 'Karla', 'Navarro', 'profesor011@example.invalid', '2025-01-10'),
('8026012', 'Luis', 'Medina', 'profesor012@example.invalid', '2025-01-10'),
('8026013', 'Marta', 'Ortega', 'profesor013@example.invalid', '2025-01-10'),
('8026014', 'Nicolas', 'Vega', 'profesor014@example.invalid', '2025-01-10'),
('8026015', 'Olga', 'Castillo', 'profesor015@example.invalid', '2025-01-10'),
('8026016', 'Pablo', 'Santos', 'profesor016@example.invalid', '2025-01-10'),
('8026017', 'Raquel', 'Cortes', 'profesor017@example.invalid', '2025-01-10'),
('8026018', 'Sergio', 'Reyes', 'profesor018@example.invalid', '2025-01-10'),
('8026019', 'Teresa', 'Molina', 'profesor019@example.invalid', '2025-01-10'),
('8026020', 'Ulises', 'Salazar', 'profesor020@example.invalid', '2025-01-10'),
('8026021', 'Valeria', 'Guzman', 'profesor021@example.invalid', '2025-01-10'),
('8026022', 'Walter', 'Pardo', 'profesor022@example.invalid', '2025-01-10'),
('8026023', 'Ximena', 'Rios', 'profesor023@example.invalid', '2025-01-10'),
('8026024', 'Yago', 'Leon', 'profesor024@example.invalid', '2025-01-10'),
('8026025', 'Zoe', 'Acosta', 'profesor025@example.invalid', '2025-01-10'),
('8026026', 'Andres', 'Diaz', 'profesor026@example.invalid', '2025-01-10'),
('8026027', 'Beatriz', 'Lopez', 'profesor027@example.invalid', '2025-01-10'),
('8026028', 'Camilo', 'Garcia', 'profesor028@example.invalid', '2025-01-10'),
('8026029', 'Diana', 'Martinez', 'profesor029@example.invalid', '2025-01-10'),
('8026030', 'Esteban', 'Rodriguez', 'profesor030@example.invalid', '2025-01-10');

UPDATE estudiantes SET direccion = CONCAT('Calle ', 20 + CAST(RIGHT(numero_documento, 3) AS UNSIGNED), ' No. 15-42, Bogotá')
WHERE numero_documento REGEXP '^1026[0-9]{3}$';
UPDATE acudientes SET direccion = CONCAT('Calle ', 20 + CAST(RIGHT(numero_documento, 3) AS UNSIGNED), ' No. 15-42, Bogotá')
WHERE numero_documento REGEXP '^5226[0-9]{3}$';

INSERT INTO estudiante_acudiente (id_estudiante, id_acudiente, es_principal)
SELECT e.id_estudiante, a.id_acudiente, TRUE
FROM estudiantes e JOIN acudientes a
  ON a.numero_documento = REPLACE(e.numero_documento, '1026', '5226')
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$';

INSERT INTO matriculas (id_estudiante, id_grado, año_academico, fecha_matricula)
SELECT e.id_estudiante, g.id_grado, 2026, '2026-01-15'
FROM estudiantes e CROSS JOIN grados g
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$' AND g.nivel_numerico = 1 + FLOOR((CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED) - 1) / 10);

INSERT INTO materias (codigo_materia, nombre_materia, id_grado, descripcion)
SELECT REPLACE(e.numero_documento, '1026', 'LHSEED-M'),
       CASE MOD(CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED) - 1, 10) WHEN 0 THEN 'Matemáticas' WHEN 1 THEN 'Lengua Castellana' WHEN 2 THEN 'Ciencias Naturales' WHEN 3 THEN 'Ciencias Sociales' WHEN 4 THEN 'Inglés' WHEN 5 THEN 'Educación Física' WHEN 6 THEN 'Educación Artística' WHEN 7 THEN 'Tecnología' WHEN 8 THEN 'Ética y Valores' WHEN 9 THEN 'Música' END, g.id_grado,
       'Asignatura del plan de estudios de primaria; datos ficticios de entrega'
FROM estudiantes e CROSS JOIN grados g
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$' AND g.nivel_numerico = 1 + FLOOR((CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED) - 1) / 10);

INSERT INTO asignaciones_profesor (id_profesor, id_materia, año_academico, seccion)
SELECT p.id_profesor, m.id_materia, 2026, 'A'
FROM profesores p JOIN materias m
  ON m.codigo_materia = REPLACE(p.numero_documento, '8026', 'LHSEED-M')
WHERE p.numero_documento REGEXP '^8026[0-9]{3}$';

INSERT INTO inscripciones_materia
    (id_estudiante, id_materia, año_academico, periodo_academico, seccion, fecha_inscripcion)
SELECT e.id_estudiante, m.id_materia, 2026, 'III', 'A', '2026-01-16'
FROM estudiantes e JOIN materias m
  ON m.codigo_materia = REPLACE(e.numero_documento, '1026', 'LHSEED-M')
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$';

INSERT INTO registros_academicos
    (id_estudiante, id_materia, id_profesor, año_academico, periodo_academico,
     valor_nota, observaciones, fecha_registro)
SELECT e.id_estudiante, m.id_materia, p.id_profesor, 2026, 'III',
       3.0 + MOD(CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED), 21) / 10.0,
       'LHSEED: nota ficticia', '2026-09-18'
FROM estudiantes e
JOIN materias m ON m.codigo_materia = REPLACE(e.numero_documento, '1026', 'LHSEED-M')
JOIN profesores p ON p.numero_documento = REPLACE(e.numero_documento, '1026', '8026')
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$';

INSERT INTO pagos
    (id_estudiante, id_acudiente, id_concepto, monto_pagado, fecha_pago,
     metodo_pago, numero_recibo, año_academico, periodo_academico, notas)
SELECT e.id_estudiante, a.id_acudiente, c.id_concepto, 350000.00, '2026-09-18',
       'Transferencia', REPLACE(e.numero_documento, '1026', 'LHSEED-REC'),
       2026, 'III', 'LHSEED: pago ficticio sin factura ni aplicación automática'
FROM estudiantes e
JOIN acudientes a ON a.numero_documento = REPLACE(e.numero_documento, '1026', '5226')
CROSS JOIN conceptos_pago c
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$' AND c.codigo_concepto = 'PENSION';

INSERT INTO clubes (nombre_club, descripcion, id_profesor_supervisor, capacidad_maxima)
SELECT CONCAT('LHSEED-Club-', RIGHT(p.numero_documento, 3)),
       CASE CAST(RIGHT(p.numero_documento, 3) AS UNSIGNED) WHEN 1 THEN 'Club de Ajedrez' WHEN 2 THEN 'Club de Robótica' WHEN 3 THEN 'Club de Lectura' WHEN 4 THEN 'Club de Teatro' WHEN 5 THEN 'Club de Coro' WHEN 6 THEN 'Club de Danza' WHEN 7 THEN 'Club de Fútbol' WHEN 8 THEN 'Club de Baloncesto' WHEN 9 THEN 'Club de Voleibol' WHEN 10 THEN 'Club de Atletismo' WHEN 11 THEN 'Club de Pintura' WHEN 12 THEN 'Club de Fotografía' WHEN 13 THEN 'Club de Periodismo' WHEN 14 THEN 'Club de Astronomía' WHEN 15 THEN 'Club de Huerta Escolar' WHEN 16 THEN 'Club de Ciencias' WHEN 17 THEN 'Club de Matemáticas' WHEN 18 THEN 'Club de Inglés' WHEN 19 THEN 'Club de Programación' WHEN 20 THEN 'Club de Debate' WHEN 21 THEN 'Club de Cine' WHEN 22 THEN 'Club de Origami' WHEN 23 THEN 'Club de Escultura' WHEN 24 THEN 'Club de Música Andina' WHEN 25 THEN 'Club de Percusión' WHEN 26 THEN 'Club de Natación' WHEN 27 THEN 'Club de Patinaje' WHEN 28 THEN 'Club de Reciclaje' WHEN 29 THEN 'Club de Senderismo' WHEN 30 THEN 'Club de Escritura' END, p.id_profesor, 15
FROM profesores p WHERE p.numero_documento REGEXP '^8026[0-9]{3}$';

INSERT INTO membresias_club (id_estudiante, id_club, fecha_ingreso)
SELECT e.id_estudiante, c.id_club, '2026-02-01'
FROM estudiantes e JOIN clubes c
  ON c.nombre_club = CONCAT('LHSEED-Club-', RIGHT(e.numero_documento, 3))
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$';

INSERT INTO recursos
    (codigo_recurso, nombre_recurso, id_categoria, descripcion, fecha_adquisicion, ubicacion_fisica)
SELECT REPLACE(e.numero_documento, '1026', 'LHSEED-R'),
       CONCAT(CASE MOD(CAST(RIGHT(e.numero_documento, 3) AS UNSIGNED) - 1, 10) WHEN 0 THEN 'El principito' WHEN 1 THEN 'La peor señora del mundo' WHEN 2 THEN 'Cuentos de la selva' WHEN 3 THEN 'Rafael Pombo: poemas infantiles' WHEN 4 THEN 'Fábulas de Esopo' WHEN 5 THEN 'Platero y yo' WHEN 6 THEN 'El libro de la selva' WHEN 7 THEN 'Alicia en el país de las maravillas' WHEN 8 THEN 'Mitos y leyendas de Colombia' WHEN 9 THEN 'Atlas escolar de Colombia' END, ' - ejemplar ', RIGHT(e.numero_documento, 3)), c.id_categoria,
       'Recurso ficticio de demostración', '2026-01-10', 'Biblioteca de prueba'
FROM estudiantes e CROSS JOIN categorias_recurso c
WHERE e.numero_documento REGEXP '^1026[0-9]{3}$' AND c.nombre_categoria = 'Libro';

-- No confirmar todavía: ejecutar docs/seed_checks.sql en esta misma conexión.
-- Si los ocho conteos son 30 y no hay incoherencias: COMMIT;
-- Ante cualquier error o conteo distinto: ROLLBACK;
