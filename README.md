# Little Horizons ERP

Aplicación Java de consola para gestionar la operación académica, extracurricular, financiera y de recursos del Colegio Little Horizons.

## Ejecutar la base del proyecto

Requiere JDK 17 y Maven. Los módulos que persistan datos requerirán MySQL 8.0 o superior.

```sh
mvn package
```

El menú valida entradas y permite salir. En **Gestión de Estudiantes** puedes listar estudiantes, registrar un acudiente y vincularlo a un estudiante nuevo usando su documento. Durante el registro puedes escribir `/volver` para cancelar.

Para probarla en IntelliJ, configura `DB_URL`, `DB_USER` y `DB_PASSWORD` en la configuración de ejecución de `Main`. Usa los datos de tu conexión de DBeaver. Por ejemplo, `DB_URL=jdbc:mysql://127.0.0.1:3306/erp_little_horizons`. Ejecuta `Main` desde el módulo Maven para incluir el driver JDBC. Si eliges listar estudiantes y la tabla está vacía, verás `Estudiantes registrados: 0` y `No hay estudiantes registrados.`

## Matrículas

En **Gestión de Estudiantes**, la opción **5** permite matricular por documento, ID del grado y año académico. La fecha se toma del día de registro y el estado inicial es `Activa`. La opción **6** muestra el historial. La restricción de MySQL impide más de una matrícula por estudiante y año, independientemente de su estado. Se aceptan años de cuatro cifras, incluidos años futuros para permitir planificación académica.

## Inscripciones en materias

En **Gestión Académica**, inscribe un estudiante por documento, año y período (I–IV). El catálogo muestra materias activas del grado de su matrícula activa. La inscripción vuelve a comprobar esas condiciones al guardar. La sección es opcional. También puedes consultar el historial por documento. MySQL impide duplicar estudiante, materia, año y período incluso si la inscripción anterior está retirada.

## Calificaciones

En **Gestión Académica → Registrar calificación**, indica documento, año y período. Se muestran las materias con inscripción activa o completada y profesor activo asignado al mismo año y sección, incluidos los casos sin sección. La nota admite coma o punto y máximo un decimal entre 0 y 5. Una nota duplicada se rechaza.

Para **Final** se admite una inscripción activa o completada en cualquier período de ese año. Es una nota ingresada manualmente. El historial muestra los promedios por año y período con el mismo peso para cada materia y redondeo a un decimal. Las notas Final se promedian por separado.

## Organización MVC

- `Main` conecta las piezas y arranca la aplicación.
- `view` muestra la interfaz de consola y lee entradas.
- `controller` coordina las acciones de la aplicación.
- `dao` ejecuta las consultas JDBC y `util` abre la conexión con MySQL.
- `model` representa los datos que usa la aplicación. `service` coordina el registro de estudiantes y la transacción con el acudiente.

## Orden de implementación

1. Revisar el DDL oficial y preparar `docs/database.sql` para `erp_little_horizons`.
2. Completar el primer flujo de estudiantes y acudientes con JDBC. Después añadir matrícula e inscripciones.
3. Añadir profesores, asignaciones, calificaciones y asistencia. Usar Streams para promedios y reportes.
4. Implementar clubes, eventos y préstamos con sus reglas de cupo y disponibilidad.
5. Modelar cobros pendientes, registrar pagos y generar el PDF de factura con elementos DIAN simulados según el enunciado.
6. Completar datos de prueba, documentación, registro de errores y verificación de la entrega.
