-- Solo lectura. Ejecutar primero y por separado del resto de las pruebas.
USE erp_little_horizons;

SELECT VERSION() AS version_mysql, DATABASE() AS base_actual,
       CURRENT_DATE AS fecha_mysql;

-- Deben aparecer las tres tablas con motor InnoDB.
SELECT TABLE_NAME, ENGINE
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('tarifas_estudiante', 'cobros_estudiante', 'aplicaciones_pago')
ORDER BY TABLE_NAME;

-- Permite revisar columnas, nulabilidad y tipos sin consultar datos personales.
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('tarifas_estudiante', 'cobros_estudiante', 'aplicaciones_pago')
ORDER BY TABLE_NAME, ORDINAL_POSITION;

SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE
FROM information_schema.TABLE_CONSTRAINTS
WHERE CONSTRAINT_SCHEMA = DATABASE()
  AND TABLE_NAME IN ('tarifas_estudiante', 'cobros_estudiante', 'aplicaciones_pago')
ORDER BY TABLE_NAME, CONSTRAINT_TYPE, CONSTRAINT_NAME;
